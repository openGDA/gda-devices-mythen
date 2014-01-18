/*-
 * Copyright © 2013 Diamond Light Source Ltd.
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

import gda.device.detector.mythen.data.MythenProcessedDataset;
import gda.device.detector.mythen.tasks.AtPointEndTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;

public class RCPPlotLastPointTask implements AtPointEndTask, InitializingBean {

	private String panelName;
	private String xAxisName;
	private String yAxisName;
	private static final Logger logger = LoggerFactory.getLogger(RCPPlotLastPointTask.class);

	@Override
	public void afterPropertiesSet() throws Exception {
		if (getPanelName() == null) {
			throw new IllegalStateException("You have not specified which panel the data should be plotted in");
		}
	}

	@Override
	public void run(String filename, MythenProcessedDataset processedData) {
		double[] angles = processedData.getAngleArray();
		double[] counts = processedData.getCountArray();

		IDataset channelsDataset = new DoubleDataset(angles, null);
		channelsDataset.setName("angle");
		IDataset countsDataset = new DoubleDataset(counts, null);
		countsDataset.setName(filename);

		try {
			SDAPlotter.plot(panelName, channelsDataset, countsDataset);
//			RCPPlotter.plot(panelName, filename, channelsDataset, countsDataset);
		} catch (Exception e) {
			logger.error("Exception throwed on RCPPlotter.plot to panel " + panelName, e);
		}

	}

	@Override
	public void run(String filename, MythenProcessedDataset processedData, boolean clearFirst) {
		double[] angles = processedData.getAngleArray();
		double[] counts = processedData.getCountArray();

		IDataset channelsDataset = new DoubleDataset(angles, null);
		channelsDataset.setName(getxAxisName());
		IDataset countsDataset = new DoubleDataset(counts, null);
		countsDataset.setName(filename);

		try {
			if (clearFirst) {
				SDAPlotter.plot(panelName, channelsDataset, new IDataset[] { countsDataset}, getxAxisName(), getyAxisName());
			} else {
				SDAPlotter.addPlot(panelName, "",new IDataset[] { channelsDataset }, new IDataset[] { countsDataset }, getxAxisName(), getyAxisName());
			}
		} catch (Exception e) {
			logger.error("Exception throwed on RCPPlotter.plot to panel " + panelName, e);
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

}
