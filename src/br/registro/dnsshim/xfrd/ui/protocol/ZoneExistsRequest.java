package br.registro.dnsshim.xfrd.ui.protocol;

import br.registro.dnsshim.util.DomainNameUtil;

public class ZoneExistsRequest extends RestrictOperation {
	private String zone;

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = DomainNameUtil.trimDot(zone.trim().toLowerCase());;
	}
}
