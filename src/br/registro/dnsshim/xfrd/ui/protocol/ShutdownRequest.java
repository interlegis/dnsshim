package br.registro.dnsshim.xfrd.ui.protocol;

public class ShutdownRequest extends RestrictOperation {
	private String secret;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

}
