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

package uk.ac.gda.devices.mythen.event;

import java.io.Serializable;

public class PlotDataFileEvent implements Serializable {
	private String filename;
	private boolean clearFirst;
	
	public PlotDataFileEvent(String filename, boolean newplot) {
		this.filename=filename;
		this.clearFirst=newplot;
	}

	public String getFilename() {
		return filename;
	}

	public boolean isClearFirst() {
		return clearFirst;
	}

}
