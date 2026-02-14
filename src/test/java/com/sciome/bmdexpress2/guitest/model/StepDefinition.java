package com.sciome.bmdexpress2.guitest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StepDefinition
{
	private String action;
	private String target;
	private String text;
	private String value;
	private List<String> path;
	private List<String> files;
	private Long millis;
	@JsonProperty("timeout_millis")
	private Long timeoutMillis;
	private String key;
	private String description;

	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	public String getTarget()
	{
		return target;
	}

	public void setTarget(String target)
	{
		this.target = target;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public List<String> getPath()
	{
		return path;
	}

	public void setPath(List<String> path)
	{
		this.path = path;
	}

	public List<String> getFiles()
	{
		return files;
	}

	public void setFiles(List<String> files)
	{
		this.files = files;
	}

	public Long getMillis()
	{
		return millis;
	}

	public void setMillis(Long millis)
	{
		this.millis = millis;
	}

	public Long getTimeoutMillis()
	{
		return timeoutMillis;
	}

	public void setTimeoutMillis(Long timeoutMillis)
	{
		this.timeoutMillis = timeoutMillis;
	}

	public String getKey()
	{
		return key;
	}

	public void setKey(String key)
	{
		this.key = key;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
