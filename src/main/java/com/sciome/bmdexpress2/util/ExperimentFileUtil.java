package com.sciome.bmdexpress2.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import com.sciome.bmdexpress2.mvp.model.DoseResponseExperiment;
import com.sciome.bmdexpress2.mvp.model.probe.Probe;
import com.sciome.bmdexpress2.mvp.model.probe.ProbeResponse;
import com.sciome.bmdexpress2.mvp.model.probe.Treatment;

/**
 * Reads tab-delimited dose-response data files into DoseResponseExperiment objects.
 *
 * bmdx-core: stripped of JavaFX dependencies (Alert dialog for header detection).
 * In headless/CLI mode, callers must specify isFirstLineHeader explicitly.
 * File header metadata parsing (ExperimentDescriptionParser) is still invoked.
 */
public class ExperimentFileUtil
{
	private static ExperimentFileUtil instance = null;

	protected ExperimentFileUtil()
	{
	}

	public static ExperimentFileUtil getInstance()
	{
		if (instance == null)
		{
			instance = new ExperimentFileUtil();
		}
		return instance;
	}

	/**
	 * Read a tab-delimited dose-response file and return a DoseResponseExperiment.
	 *
	 * The file format is BMDExpress's standard pivot layout:
	 *   Row 0 (optional header): probe_id  colname1  colname2  ...
	 *   Row 1 (dose row):        label     dose1     dose2     ...
	 *   Row 2+:                  probeId   value1    value2    ...
	 *
	 * @param infile             The input file (tab-delimited .txt or .csv)
	 * @param isFirstLineHeader  Whether the first line contains column headers
	 * @return A DoseResponseExperiment, or null if parsing fails
	 */
	public DoseResponseExperiment readFile(File infile, boolean isFirstLineHeader)
	{
		try
		{
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr, 1024 * 2000);
			Vector<String[]> vecData = new Vector<String[]>();
			StringBuffer bfNotes = new StringBuffer();
			String line = "";
			int c = 0;

			DoseResponseExperiment doseResponseExperiment = new DoseResponseExperiment();

			try
			{
				while ((line = br.readLine()) != null)
				{
					if (line.indexOf("\t") >= 0 && !line.replaceAll("\\s*", "").equals(""))
					{
						String[] array = line.split("\t");
						int n = array.length;
						vecData.add(array);

						if (n > c)
						{
							c = n;
						}
					}
					else
					{
						bfNotes.append(line + "\n");
					}
				}

				String[] headers = new String[c];
				for (int j = 0; j < c; j++)
				{
					headers[j] = "Column " + j;
				}

				if (vecData.size() > 1)
				{
					String[] experimentHeaders = vecData.get(0);

					// If isFirstLineHeader is false, auto-detect: if any non-first
					// column in the first row is non-numeric, treat it as a header.
					int starti = 0;
					if (isFirstLineHeader || autoDetectHeader(experimentHeaders))
					{
						starti = 1;
					}

					List<Treatment> treatments = new ArrayList<>();
					for (int i = 1; i < experimentHeaders.length; i++)
					{
						Float dose = Float.valueOf(vecData.get(starti)[i]);
						String colheader = experimentHeaders[i];
						if (starti == 0)
						{
							colheader = headers[i - 1];
						}
						Treatment treatment = new Treatment(colheader, dose);
						treatments.add(treatment);
					}

					// Sort treatments by dose and track the reordering
					List<Treatment> orderedTreatments = new ArrayList<>(treatments);
					Collections.sort(orderedTreatments, new Comparator<Treatment>() {
						@Override
						public int compare(Treatment o1, Treatment o2)
						{
							return o1.getDose().compareTo(o2.getDose());
						}
					});

					List<Integer> orderedIndexes = new ArrayList<>(treatments.size());
					for (Treatment t : treatments)
					{
						orderedIndexes.add(orderedTreatments.indexOf(t));
					}

					List<ProbeResponse> probeResponses = new ArrayList<>();
					for (int i = starti + 1; i < vecData.size(); i++)
					{
						String probeID = vecData.get(i)[0];
						Probe probe = new Probe();
						probe.setId(probeID);

						ProbeResponse probeResponse = new ProbeResponse();
						probeResponse.setProbe(probe);
						List<Float> responseRow = new ArrayList<>();

						for (int j = 1; j < vecData.get(i).length; j++)
						{
							try
							{
								Float doseResponse = Float.valueOf(vecData.get(i)[j]);
								responseRow.add(doseResponse);
							}
							catch (Exception e)
							{
								System.err.println("Non-numeric value on line " + (i + 1) +
									", column " + (j + 1) + " of file \"" + infile.getName() + "\"");
								return null;
							}
						}

						if (responseRow.size() != treatments.size())
						{
							System.err.println("Column count mismatch on line " + (i + 1) +
								" of file \"" + infile.getName() + "\"");
							return null;
						}

						// Reorder responses to match sorted treatments
						List<Float> orderedResponses = new ArrayList<>(responseRow.size());
						for (int idx = 0; idx < responseRow.size(); idx++)
						{
							orderedResponses.add(null);
						}
						int j = 0;
						for (Float response : responseRow)
						{
							orderedResponses.set(orderedIndexes.get(j).intValue(), response);
							j++;
						}

						probeResponse.setResponses(orderedResponses);
						probeResponses.add(probeResponse);
					}

					doseResponseExperiment.setTreatments(orderedTreatments);
					doseResponseExperiment.setProbeResponses(probeResponses);

					String fileName = infile.getName();
					if (fileName.indexOf(".") > 0)
						fileName = fileName.substring(0, fileName.lastIndexOf("."));
					doseResponseExperiment.setName(fileName);

					// Parse experimental metadata from file header
					ExperimentDescriptionParser.ParseResult parseResult =
						ExperimentDescriptionParser.parseFromFile(infile);
					doseResponseExperiment.setExperimentDescription(parseResult.getDescription());

					return doseResponseExperiment;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				br.close();
				fr.close();
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Auto-detect whether the first row is a header by checking if any
	 * non-first column value is non-numeric.
	 */
	private boolean autoDetectHeader(String[] headers)
	{
		for (int i = 1; i < headers.length; i++)
		{
			try
			{
				Double.parseDouble(headers[i]);
			}
			catch (NumberFormatException nfe)
			{
				return true;
			}
		}
		return false;
	}
}
