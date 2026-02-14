package com.sciome.bmdexpress2.guitest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SetupDefinition
{
	@JsonProperty("stub_files")
	private List<StubFile> stubFiles;

	public List<StubFile> getStubFiles()
	{
		return stubFiles;
	}

	public void setStubFiles(List<StubFile> stubFiles)
	{
		this.stubFiles = stubFiles;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class StubFile
	{
		private String source;
		@JsonProperty("as")
		private String targetPath;

		public String getSource()
		{
			return source;
		}

		public void setSource(String source)
		{
			this.source = source;
		}

		public String getTargetPath()
		{
			return targetPath;
		}

		public void setTargetPath(String targetPath)
		{
			this.targetPath = targetPath;
		}
	}
}
