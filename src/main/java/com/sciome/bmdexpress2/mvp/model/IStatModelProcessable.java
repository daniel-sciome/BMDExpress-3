package com.sciome.bmdexpress2.mvp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.sciome.bmdexpress2.mvp.model.probe.EndpointResponse;
import com.sciome.bmdexpress2.mvp.model.probe.ProbeResponse;

/**
 * Contract for any dataset that can be sent to BMD statistical model fitting.
 *
 * In the current system, both DoseResponseExperiment (raw data) and prefilter
 * results (OneWayANOVA, WilliamsTrend, etc.) implement this interface so the
 * same modeling code can process either.
 *
 * The interface provides both genomics-named methods (getProcessableProbeResponses)
 * and domain-neutral aliases (getProcessableEndpointResponses) for compatibility
 * across data domains.
 */
public interface IStatModelProcessable
{
	public DoseResponseExperiment getProcessableDoseResponseExperiment();

	/**
	 * @deprecated Use {@link #getProcessableEndpointResponses()} for domain-agnostic code.
	 *             This method remains for genomics-specific callers.
	 */
	@Deprecated(forRemoval = false)
	public List<ProbeResponse> getProcessableProbeResponses();

	/**
	 * Domain-neutral alias for {@link #getProcessableProbeResponses()}.
	 *
	 * Returns the endpoint responses (probes, clinical measurements, etc.)
	 * that should be sent to BMD model fitting.  Default implementation
	 * delegates to getProcessableProbeResponses().
	 *
	 * @JsonIgnore because this returns the same list as getProcessableProbeResponses() —
	 * Jackson would serialize it as redundant @ref integer arrays, cluttering the JSON tree.
	 */
	@JsonIgnore
	public default List<? extends EndpointResponse> getProcessableEndpointResponses()
	{
		return getProcessableProbeResponses();
	}

	public String getParentDataSetName();

	public String getDataSetName();

	public LogTransformationEnum getLogTransformation();

}
