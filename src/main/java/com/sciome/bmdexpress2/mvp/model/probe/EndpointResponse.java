package com.sciome.bmdexpress2.mvp.model.probe;

import java.util.List;

/**
 * Domain-neutral interface for a single endpoint's dose-response data.
 *
 * Each EndpointResponse pairs an {@link Endpoint} (what was measured) with
 * a list of response values (one per treatment/animal in the experiment).
 * Together, a collection of EndpointResponses forms the dose-response matrix
 * for a {@link com.sciome.bmdexpress2.mvp.model.DoseResponseExperiment}.
 *
 * In genomics, this is a row in the expression matrix (one probe across all
 * samples).  In apical toxicology, this is one endpoint (e.g., ALT) measured
 * across all dose groups.
 *
 * {@link ProbeResponse} implements this interface, so all existing code
 * continues to work.  New domain-agnostic code should prefer EndpointResponse.
 *
 * @see ProbeResponse
 * @see Endpoint
 */
public interface EndpointResponse
{
	/**
	 * Returns the endpoint (probe, clinical measurement, etc.) this response belongs to.
	 */
	Endpoint getEndpoint();

	/**
	 * Returns the measured values, one per treatment in the experiment's dose schedule.
	 */
	List<Float> getResponses();

	/**
	 * Sets the measured values. Implementations may also update internal caches
	 * (e.g., byte blob for serialization, float array for model fitting).
	 */
	void setResponses(List<Float> responses);

	/**
	 * Returns the response values as a primitive float array for efficient
	 * model fitting. May be null if not yet initialized.
	 */
	float[] getResponseArray();
}
