package com.sciome.bmdexpress2.guitest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sciome.bmdexpress2.guitest.model.WorkflowDefinition;

public class WorkflowLoader
{
	private static final String WORKFLOWS_DIR = "/workflows/";
	private final ObjectMapper mapper;

	public WorkflowLoader()
	{
		this.mapper = new ObjectMapper(new YAMLFactory());
	}

	public WorkflowDefinition load(String resourceName) throws IOException
	{
		String path = WORKFLOWS_DIR + resourceName;
		try (InputStream is = getClass().getResourceAsStream(path))
		{
			if (is == null)
			{
				throw new IOException("Workflow resource not found: " + path);
			}
			return mapper.readValue(is, WorkflowDefinition.class);
		}
	}

	public List<WorkflowDefinition> loadAll() throws IOException
	{
		List<WorkflowDefinition> workflows = new ArrayList<>();
		for (String name : listAvailable())
		{
			workflows.add(load(name));
		}
		return workflows;
	}

	public List<String> listAvailable()
	{
		List<String> names = new ArrayList<>();
		try
		{
			URL dirUrl = getClass().getResource(WORKFLOWS_DIR);
			if (dirUrl == null)
			{
				return names;
			}

			URI uri = dirUrl.toURI();
			Path dirPath;
			if ("jar".equals(uri.getScheme()))
			{
				FileSystem fs;
				try
				{
					fs = FileSystems.getFileSystem(uri);
				}
				catch (FileSystemNotFoundException e)
				{
					fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
				}
				dirPath = fs.getPath(WORKFLOWS_DIR);
			}
			else
			{
				dirPath = Paths.get(uri);
			}

			try (Stream<Path> stream = Files.list(dirPath))
			{
				stream.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
						.forEach(p -> names.add(p.getFileName().toString()));
			}
		}
		catch (URISyntaxException | IOException e)
		{
			System.err.println("Warning: Could not scan workflows directory: " + e.getMessage());
		}
		Collections.sort(names);
		return names;
	}
}
