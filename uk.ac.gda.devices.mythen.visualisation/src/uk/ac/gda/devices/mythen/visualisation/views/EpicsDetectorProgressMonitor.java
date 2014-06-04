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

import gda.device.DeviceException;
import gda.device.scannable.EpicsScannable;
import gda.observable.IObserver;
import gov.aps.jca.event.MonitorListener;

import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import uk.ac.gda.client.hrpd.epicsdatamonitor.EpicsDoubleDataListener;
/**
 * A progress monitor for monitoring or reporting an EPICS process progress state.
 * It provides a label displaying the task and subtask name, and a progress indicator to show progress.
 * The progress reporting is driven by EPICS events monitored via {@link MonitorListener} instances,
 * <p>
 * To create an instance of this class, one must provide:
 * <li> a total work listener using <code>setTotalWorkListener(EpicsIntegerDataListener)</code>;</li>
 * <li> a work listener using <code>setWorkedSoFarListener(EpicsIntegerDataListener)</code>;</li>
 * <br>
 * You may optionally provide:
 * <li> a message listener using <code>setMessageListener(EpicsStringDataListener)</code> if there are messages related to the EPICS process;</li>
 * <li> a STOP scannable using <code>setStopScannable(EpicsScannable)</code> if you want the CANCEL operation to stop EPICS process;</li>
 *  
 */

public class EpicsDetectorProgressMonitor extends ProgressMonitorPart implements IObserver, InitializingBean{
	private static final Logger logger = LoggerFactory.getLogger(EpicsDetectorProgressMonitor.class);
	//Spring configurable properties
	private EpicsDoubleDataListener exposureTimeListener;
	private EpicsDoubleDataListener timeRemainingListener;
	private EpicsScannable stopScannable; //optional if no Cancel, 
	private Button stopButton;
	private boolean hasStopButton = false;

	public EpicsDetectorProgressMonitor(Composite parent, Layout layout, boolean allowStopButton) {
		super(parent, layout);
		// cannot use base class stopButton due to additional action to stop EPICS process.
		this.hasStopButton = allowStopButton;
		initialize(layout, SWT.DEFAULT);
	}

	SelectionAdapter listener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
				try {
					if (getStopScannable()!=null) {
						getStopScannable().moveTo(0);
					}
	    			if (stopButton != null) {
	    				stopButton.setEnabled(false);
	    			}				
				} catch (DeviceException e1) {
					logger.error("Failed to stop EPICS operation.", e1);
				}
    			// on cancel operation, must finish beginTask
				done(); 
				lastWorkedTo=0;
				totalWork=0;
		}
	};

	/**
	 * Creates the progress monitor's UI parts and layouts them according to the given layout. If the layout is
	 * <code>null</code> the part's default layout is used.
	 * 
	 * @param layout
	 *            The layout for the receiver.
	 * @param progressIndicatorHeight
	 *            The suggested height of the indicator
	 */
	@Override
	protected void initialize(Layout layout, int progressIndicatorHeight) {
		if (layout == null) {
			GridLayout l = new GridLayout();
			l.marginWidth = 0;
			l.marginHeight = 0;
			layout = l;
		}
		int numColumns = 1;
		if (hasStopButton)
			numColumns++;
		setLayout(layout);

		if (layout instanceof GridLayout)
			((GridLayout) layout).numColumns = numColumns;

		fLabel = new Label(this, SWT.LEFT);
		fLabel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, numColumns, 1));

		if (progressIndicatorHeight == SWT.DEFAULT) {
			GC gc = new GC(fLabel);
			FontMetrics fm = gc.getFontMetrics();
			gc.dispose();
			progressIndicatorHeight = fm.getHeight();
		}

		fProgressIndicator = new ProgressIndicator(this);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = false;
		gd.verticalAlignment = GridData.CENTER;
		gd.heightHint = progressIndicatorHeight;
		fProgressIndicator.setLayoutData(gd);

		if (hasStopButton) {
			stopButton = new Button(this, SWT.PUSH);
			stopButton.addSelectionListener(listener);
			final Image stopImage = ImageDescriptor
					.createFromFile(EpicsDetectorProgressMonitor.class, "images/stop.gif").createImage(getDisplay()); //$NON-NLS-1$
			final Cursor arrowCursor = new Cursor(this.getDisplay(), SWT.CURSOR_ARROW);
			stopButton.setCursor(arrowCursor);
			stopButton.setImage(stopImage);
			stopButton.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					stopImage.dispose();
					arrowCursor.dispose();
				}
			});
			stopButton.setEnabled(false);
			stopButton.setToolTipText("Cancel operation"); //$NON-NLS-1$
			attachToCancelComponent(stopButton);
		}
	}

	public void addIObservers() {
		if (getExposureTimeListener()!=null) {
			getExposureTimeListener().addIObserver(this);
		}
		if (getTimeRemainingListener()!=null) {
			getTimeRemainingListener().addIObserver(this);
		}
	}
	
	int lastWorkedTo=0;
	int totalWork=0;
	@Override
	public void update(Object source, Object arg) {
		if (source==getExposureTimeListener() && arg instanceof Double) {
			totalWork=(int) ((double) arg*1000);
			if (totalWork!=0) {
				beginTask(getTaskName(), totalWork);
				stopButton.setEnabled(true);
			}
		} else if(source==getTimeRemainingListener() && arg instanceof Double) {
			int timeremain=(int) ((double) arg*1000);
			int workedSoFar=totalWork-timeremain;
			if (workedSoFar<totalWork) {
				worked(workedSoFar-lastWorkedTo);
				lastWorkedTo=workedSoFar;
			} else {
				done();
				lastWorkedTo=0;
				totalWork=0;
			}
		} 
	}

	private String getTaskName() {
		return this.fTaskName;
	}

	public EpicsScannable getStopScannable() {
		return stopScannable;
	}

	public void setStopScannable(EpicsScannable stopScannable) {
		this.stopScannable = stopScannable;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (getExposureTimeListener()==null) {
			throw new IllegalArgumentException("exposureTimeListener must not be null.");
		}
		if (getTimeRemainingListener()==null) {
			throw new IllegalArgumentException("timeRemainingListener must not be null.");
		}
		if (hasStopButton && stopScannable==null) {
			throw new IllegalArgumentException("stopScannable must not be null.");
		}
	}

	public EpicsDoubleDataListener getExposureTimeListener() {
		return exposureTimeListener;
	}

	public void setExposureTimeListener(EpicsDoubleDataListener exposureTimeListener) {
		this.exposureTimeListener = exposureTimeListener;
	}

	public EpicsDoubleDataListener getTimeRemainingListener() {
		return timeRemainingListener;
	}

	public void setTimeRemainingListener(EpicsDoubleDataListener timeRemainingListener) {
		this.timeRemainingListener = timeRemainingListener;
	}
}