/*-
 * Copyright © 2010 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.gda.devices.mythen.visualisation.tasks;

import gda.analysis.Plotter;
import gda.data.fileregistrar.FileRegistrarHelper;
import gda.device.Detector;
import gda.device.DeviceException;
import gda.device.detector.mythen.data.DataConverter;
import gda.device.detector.mythen.data.MythenDataFileUtils;
import gda.device.detector.mythen.data.MythenDataFileUtils.FileType;
import gda.device.detector.mythen.data.MythenSum;
import gda.device.detector.mythen.tasks.DataProcessingTask;
import gda.jython.InterfaceProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.gda.devices.mythen.epics.MythenDetector;

public class RCPPlotSummingDataTask implements DataProcessingTask, InitializingBean {
	
	private static final Logger logger = LoggerFactory.getLogger(RCPPlotSummingDataTask.class);
	
	protected double step = 0.004;
	
	/**
	 * Sets the angle step to use when summing the data.
	 */
	public void setStep(double step) {
		this.step = step;
	}
	
	public double getStep() {
		return step;
	}
	
	protected String panelName;

	private String xAxisName;

	private String yAxisName;
	
	protected void sumProcessedData(Detector detector)  throws DeviceException {
		ArrayList<File> files=new ArrayList<File>();
		int numberOfModules;
		DataConverter dataConverter;
		File dataDirectory;
		String summedFilename;
		if (detector instanceof MythenDetector) {
			MythenDetector mydetector=(MythenDetector)detector;
			files=mydetector.getProcessedDataFilesForThisScan();
			numberOfModules=mydetector.getNumberOfModules();
			dataConverter=mydetector.getDataConverter();
			dataDirectory=mydetector.getDataDirectory();
			summedFilename = mydetector.buildFilename("summed", FileType.PROCESSED);
		} else {
			throw new RuntimeException("summing processed data is not supported for detcetor "+detector.getName());
		}
		logger.info(String.format("Going to sum %d dataset(s)", files.size()));
		
		// Build filename of each processed data file
		String[] filenames = new String[files.size()];
		for (int i=1; i<=files.size(); i++) {
			filenames[i-1] = files.get(i-1).getAbsolutePath();
		}
		
		// Load all processed data files
		logger.info("Loading processed data...");
		double[][][] allData = MythenDataFileUtils.readMythenProcessedDataFiles(filenames);
		logger.info("Done");
		
		// Sum the data
		logger.info("Summing data...");
		print("Summing data ...");
		double[][] summedData = MythenSum.sum(allData, numberOfModules, dataConverter.getBadChannelProvider(), step);
		logger.info("Done");
		// Save the summed data
		File summedDataFile = new File(dataDirectory, summedFilename);
		logger.info(String.format("Saving summed data to %s", summedDataFile.getAbsolutePath()));
		print("Saving summed data to "+ summedDataFile.getAbsolutePath());
		try {
			MythenDataFileUtils.saveProcessedDataFile(summedData, summedDataFile.getAbsolutePath());
			logger.info("Summed data saved successfully");
		} catch (IOException e) {
			final String msg = String.format(
				"Unable to save summed data to %s, but all individual data files have been saved successfully",
				summedDataFile);
			logger.error(msg, e);
			throw new DeviceException(msg, e);
		}
		
		// Register summed data file
		FileRegistrarHelper.registerFile(summedDataFile.getAbsolutePath());
		
		// Plot summed data
		final int numChannels = summedData.length;
		double[] angles = new double[numChannels];
		double[] counts = new double[numChannels];
		for (int i=0; i<numChannels; i++) {
			angles[i] = summedData[i][0];
			counts[i] = summedData[i][1];
		}
		String name2 = FilenameUtils.getName(summedDataFile.getAbsolutePath());
		DoubleDataset anglesDataset = new DoubleDataset(angles);
		anglesDataset.setName("angle");
		DoubleDataset countsDataset = new DoubleDataset(counts);
		countsDataset.setName(name2);
		// Swing plot panel
		Plotter.plot(panelName, anglesDataset, countsDataset);
		try {
			//RCP plot panel
			SDAPlotter.plot(panelName, anglesDataset, countsDataset);
		} catch (Exception e) {
			logger.error("RCP plot failed.", e);
			throw new DeviceException("RCP plot failed.", e);
		}
	}
	@Override
	public void run(Detector detector) throws DeviceException {
		sumProcessedData(detector);
	}
	@Override
	public void afterPropertiesSet() throws Exception {
		if (getPanelName() == null) {
			throw new IllegalStateException("You have not specified which panel the data should be plotted in");
		}
	}

	public String getPanelName() {
		return panelName;
	}

	public void setPanelName(String panelName) {
		this.panelName = panelName;
	}

	public String getxAxisName() {
		return xAxisName;
	}

	public void setxAxisName(String xAxisName) {
		this.xAxisName = xAxisName;
	}

	public String getyAxisName() {
		return yAxisName;
	}

	public void setyAxisName(String yAxisName) {
		this.yAxisName = yAxisName;
	}
	/**
	 * method to print message to the Jython Terminal console.
	 * 
	 * @param msg
	 */
	private void print(String msg) {
		if (InterfaceProvider.getTerminalPrinter() != null) {
			InterfaceProvider.getTerminalPrinter().print(msg);
		}
	}

}