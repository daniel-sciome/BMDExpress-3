package com.sciome.bmdexpress2.guitest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowDefinition
{
	private String name;
	private String description;
	private SetupDefinition setup;
	private List<StepDefinition> steps;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public SetupDefinition getSetup()
	{
		return setup;
	}

	public void setSetup(SetupDefinition setup)
	{
		this.setup = setup;
	}

	public List<StepDefinition> getSteps()
	{
		return steps;
	}

	public void setSteps(List<StepDefinition> steps)
	{
		this.steps = steps;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
