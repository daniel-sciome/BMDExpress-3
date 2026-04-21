package com.sciome.bmdexpress2.mvp.model.probe;

/**
 * Domain-neutral interface for a measured quantity in a dose-response experiment.
 *
 * In genomics, an endpoint is a gene probe (microarray spot, S1500+ target, etc.).
 * In apical toxicology, an endpoint is a clinical measurement (ALT, body weight,
 * liver weight, etc.).  This interface abstracts over both — any object that has
 * a string identifier can serve as an endpoint.
 *
 * {@link Probe} implements this interface, so all existing genomics code continues
 * to work via either the Probe or Endpoint type.  New domain-agnostic code should
 * prefer the Endpoint interface.
 *
 * @see Probe
 * @see EndpointResponse
 */
public interface Endpoint
{
	/**
	 * Returns the unique identifier for this endpoint.
	 *
	 * For genomics: the probe set ID (e.g., "ACAA1A_7954").
	 * For apical data: the endpoint name (e.g., "Alanine aminotransferase").
	 */
	String getId();

	/**
	 * Sets the unique identifier for this endpoint.
	 */
	void setId(String id);
}
