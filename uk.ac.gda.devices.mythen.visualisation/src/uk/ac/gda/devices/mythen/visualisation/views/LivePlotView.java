package uk.ac.gda.devices.mythen.visualisation.views;

import gda.device.scannable.EpicsScannable;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.gda.client.hrpd.epicsdatamonitor.EpicsDoubleDataListener;


/**
 * Live plotting of detector data during acquisition. 
 */
public class LivePlotView extends ViewPart {

	private static final Logger logger = LoggerFactory.getLogger(LivePlotView.class);

	private String plotName;
	private double xAxisMin=0.000;
	private double xAxisMax=100.000;
	private String eventAdminName;
	private IRunnableWithProgress epicsProgressMonitor;
	
	private LivePlotComposite plotComposite;

	private EpicsDetectorProgressMonitor progressMonitor;
	private EpicsDoubleDataListener exposureTimeListener;
	private EpicsDoubleDataListener timeRemainingListener;
	private EpicsScannable stopScannable;	

	public LivePlotView() {
		setTitleToolTip("live display of 1D detector data");
		// setContentDescription("A view for displaying integrated spectrum.");
	}

	@Override
	public void createPartControl(Composite parent) {
		setPartName(getPlotName());
		Composite rootComposite = new Composite(parent, SWT.NONE);
		
		FillLayout layout = new FillLayout();
		layout.type=SWT.VERTICAL;
		rootComposite.setLayout(layout);

		try {
			plotComposite = new LivePlotComposite(this, rootComposite, SWT.None);
			plotComposite.setPlotName(getPlotName());
			plotComposite.setxAxisMin(getxAxisMin());
			plotComposite.setxAxisMax(getxAxisMax());
			plotComposite.setEventAdminName(eventAdminName);
			plotComposite.initialise();
			progressMonitor=new EpicsDetectorProgressMonitor(rootComposite, null, true);
			progressMonitor.setExposureTimeListener(exposureTimeListener);
			progressMonitor.setTimeRemainingListener(timeRemainingListener);
			progressMonitor.setStopScannable(getStopScannable());
			progressMonitor.addIObservers();
		} catch (Exception e) {
			logger.error("Cannot create live plot composite.", e);
		}
	}
	
	@Override
	public void setFocus() {
		plotComposite.setFocus();
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


	public IRunnableWithProgress getEpicsProgressMonitor() {
		return epicsProgressMonitor;
	}

	public void setEpicsProgressMonitor(IRunnableWithProgress epicsProgressMonitor) {
		this.epicsProgressMonitor = epicsProgressMonitor;
	}

	public EpicsScannable getStopScannable() {
		return stopScannable;
	}

	public void setStopScannable(EpicsScannable stopScannable) {
		this.stopScannable = stopScannable;
	}

	public String getEventAdminName() {
		return eventAdminName;
	}

	public void setEventAdminName(String eventAdminName) {
		this.eventAdminName = eventAdminName;
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
