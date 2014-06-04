/*-
 * Copyright © 2014 Diamond Light Source Ltd.
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

package uk.ac.gda.devices.mythen.visualisation.views;

import gda.device.detector.mythen.data.MythenDataFileUtils;
import gda.factory.Finder;
import gda.jython.scriptcontroller.Scriptcontroller;
import gda.observable.IObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FilenameUtils;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.gda.devices.mythen.visualisation.event.PlotDataFileEvent;

/**
 * Live plot composite for plotting detector data from the data file notified by the server data collection process.
 */
public class LivePlotComposite extends Composite implements IObserver {
	private Logger logger = LoggerFactory.getLogger(LivePlotComposite.class);
	private String PLOT_TITLE = "Live Detector Data";
	private String plotName = "DetectorData";
	private double xAxisMin = 0.000;
	private double xAxisMax = 100.000;

	private String eventAdminName;
	private IRunnableWithProgress epicsProgressMonitor;

	private Scriptcontroller eventAdmin; // used for passing event from server to client without the need to
												// CORBArise this class.
	private IPlottingSystem plottingSystem;
	private IWorkbenchPart workbenchpart;
	private ExecutorService executor;

	public LivePlotComposite(IWorkbenchPart part, Composite parent, int style) throws Exception {
		super(parent, style);
		this.workbenchpart = part;
		this.setBackground(ColorConstants.white);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		this.setLayout(layout);

		Composite plotComposite = new Composite(this, SWT.None);
		plotComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		plotComposite.setLayout(new FillLayout());

		plottingSystem = PlottingFactory.createPlottingSystem();
		plottingSystem.createPlotPart(plotComposite, getPlotName(), part instanceof IViewPart ? ((IViewPart) part)
				.getViewSite().getActionBars() : null, PlotType.XY, part);
		plottingSystem.setTitle(PLOT_TITLE);
		plottingSystem.getSelectedYAxis().setFormatPattern("######.#");
		plottingSystem.getSelectedXAxis().setFormatPattern("###.###");
		plottingSystem.setShowLegend(true);
		plottingSystem.getSelectedXAxis().setRange(getxAxisMin(), getxAxisMax());
	}

	public void initialise() {
		if (getEventAdminName() != null) {
			// optional file name observing
			eventAdmin = Finder.getInstance().find(getEventAdminName());
			if (eventAdmin != null) {
				eventAdmin.addIObserver(this); // observe server mythen detector task processes 
				logger.debug("Data filename observer added via script controller {}", getEventAdminName());
				executor=Executors.newSingleThreadExecutor();
			} else {
				logger.debug("Cannot find the script controller {} to add data filename observer",
						getEventAdminName());
			}
		}
	}

	@Override
	public void dispose() {
		executor.shutdown();
		boolean terminated;
		try {
			terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
			if (!terminated) {
				throw new TimeoutException("Timed out waiting for plotting data file.t");
			}
		} catch (InterruptedException | TimeoutException e) {
			logger.error("Unable to plot data", e);
			throw new RuntimeException("Unable to plot data from data file.", e);
		} 
		// clean up resources used.
		if (!plottingSystem.isDisposed()) {
			plottingSystem.clear();
		}
		plottingSystem.dispose();
		// remove reference
		super.dispose();
	}

	public void clearPlots() {
		getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				plottingSystem.setTitle("");
				plottingSystem.reset();
			}
		});
	}

	@Override
	public boolean setFocus() {
		plottingSystem.setFocus();
		return super.setFocus();
	}

	List<AbstractDataset> plotDatasets = new ArrayList<AbstractDataset>();
	private void plotFinalData(final String filename, final boolean clearFirst) {

		double[][] data = MythenDataFileUtils.readMythenProcessedDataFile(filename, false);
		final int numChannels = data.length;
		double[] angles = new double[numChannels];
		double[] counts = new double[numChannels];
		double[] errors = new double[numChannels];
		for (int i = 0; i < numChannels; i++) {
			angles[i] = data[i][0];
			counts[i] = data[i][1];
			errors[i] = data[i][2];
		}
		DoubleDataset x = new DoubleDataset(angles);
		DoubleDataset y = new DoubleDataset(counts);
		DoubleDataset error = new DoubleDataset(errors);
		y.setError(error);
		y.setName(FilenameUtils.getName(filename));
		if (clearFirst) {
			plotDatasets.clear();
			plottingSystem.clear();
		}
		plotDatasets.add(y);
		plottingSystem.createPlot1D(x, plotDatasets, PLOT_TITLE, new NullProgressMonitor());
	}

	public String getPlotName() {
		return plotName;
	}

	public void setPlotName(String plotName) {
		this.plotName = plotName;
	}

	public double getxAxisMin() {
		return xAxisMin;
	}

	public void setxAxisMin(double xAxisMin) {
		this.xAxisMin = xAxisMin;
	}

	public double getxAxisMax() {
		return xAxisMax;
	}

	public void setxAxisMax(double xAxisMax) {
		this.xAxisMax = xAxisMax;
	}

	@Override
	public void update(Object source, Object arg) {
		if (eventAdmin != null && source == eventAdmin && arg instanceof PlotDataFileEvent) {
			final String filename = ((PlotDataFileEvent) arg).getFilename();
			final boolean clearFirst = ((PlotDataFileEvent) arg).isClearFirst();
			Runnable command = new Runnable() {
				
				@Override
				public void run() {
					plotFinalData(filename, clearFirst);
				}
			};
			executor.execute(command);
		}
	}

	public String getEventAdminName() {
		return eventAdminName;
	}

	public IRunnableWithProgress getEpicsProgressMonitor() {
		return epicsProgressMonitor;
	}

	public void setEpicsProgressMonitor(IRunnableWithProgress epicsProgressMonitor) {
		this.epicsProgressMonitor = epicsProgressMonitor;
	}

	public void setEventAdminName(String eventAdminName) {
		this.eventAdminName = eventAdminName;
	}

}