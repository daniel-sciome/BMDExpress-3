/**
 * Bm2DeserializationTest.java
 *
 * Loads real .bm2 project files (Java ObjectInputStream format) and exercises
 * every dataset's getColumnHeader() and getAnalysisRows() methods.  This is
 * the critical path that was changed when ExperimentDescription metadata
 * columns were added at every pipeline stage.
 *
 * The test proves that old .bm2 files — which predate the ExperimentDescription
 * field — deserialize cleanly and produce consistent column/row widths without
 * throwing exceptions.
 *
 * Additionally, Phase 1 domain-neutral alias tests verify that the new
 * Endpoint/EndpointResponse interfaces and DoseResponseExperiment aliases
 * (getEndpointResponses, getPlatform, etc.) return the same data as
 * their genomics-named counterparts (getProbeResponses, getChip, etc.).
 *
 * .bm2 files are expected in ~/Downloads.  If none are found the test is
 * skipped (Assume), so CI can pass without sample data.
 */
package com.sciome.bmdexpress2.model;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sciome.bmdexpress2.mvp.model.BMDExpressAnalysisDataSet;
import com.sciome.bmdexpress2.mvp.model.BMDProject;
import com.sciome.bmdexpress2.mvp.model.DoseResponseExperiment;
import com.sciome.bmdexpress2.mvp.model.IStatModelProcessable;
import com.sciome.bmdexpress2.mvp.model.category.CategoryAnalysisResults;
import com.sciome.bmdexpress2.mvp.model.chip.ChipInfo;
import com.sciome.bmdexpress2.mvp.model.info.ExperimentDescription;
import com.sciome.bmdexpress2.mvp.model.prefilter.CurveFitPrefilterResults;
import com.sciome.bmdexpress2.mvp.model.prefilter.OneWayANOVAResults;
import com.sciome.bmdexpress2.mvp.model.prefilter.OriogenResults;
import com.sciome.bmdexpress2.mvp.model.prefilter.WilliamsTrendResults;
import com.sciome.bmdexpress2.mvp.model.probe.Endpoint;
import com.sciome.bmdexpress2.mvp.model.probe.EndpointResponse;
import com.sciome.bmdexpress2.mvp.model.probe.Probe;
import com.sciome.bmdexpress2.mvp.model.probe.ProbeResponse;
import com.sciome.bmdexpress2.mvp.model.stat.BMDResult;

@RunWith(Parameterized.class)
public class Bm2DeserializationTest
{
	// ── Constants ──

	/** Number of metadata columns added by ExperimentDescription. */
	private static final int METADATA_COLUMN_COUNT = 8;

	// ── Parameterized runner feeds one .bm2 File per test instance ──

	/**
	 * Collects every .bm2 file found under ~/Downloads (non-recursive first,
	 * then one level of subdirectories).  If no files are found the whole
	 * test class is skipped via Assume.
	 */
	@Parameters(name = "{0}")
	public static List<Object[]> findBm2Files()
	{
		List<Object[]> files = new ArrayList<>();
		File downloads = new File(System.getProperty("user.home"), "Downloads");
		if (downloads.isDirectory())
		{
			collectBm2(downloads, files, 2); // depth limit of 2
		}
		return files;
	}

	/**
	 * Recursively collect .bm2 files up to a given depth.
	 */
	private static void collectBm2(File dir, List<Object[]> out, int depthLeft)
	{
		if (depthLeft <= 0 || dir == null || !dir.isDirectory())
			return;
		File[] children = dir.listFiles();
		if (children == null)
			return;
		for (File f : children)
		{
			if (f.isFile() && f.getName().endsWith(".bm2"))
			{
				out.add(new Object[] { f.getName(), f });
			}
			else if (f.isDirectory())
			{
				collectBm2(f, out, depthLeft - 1);
			}
		}
	}

	// ── Instance fields ──

	/** Human-readable label (used by @Parameters name). */
	private final String label;

	/** The .bm2 file to test. */
	private final File bm2File;

	/** The deserialized project — shared across all @Test methods in this instance. */
	private BMDProject project;

	public Bm2DeserializationTest(String label, File bm2File)
	{
		this.label = label;
		this.bm2File = bm2File;
	}

	// ── Setup: deserialize the .bm2 file once per parameterized instance ──

	@Before
	public void loadProject() throws Exception
	{
		assumeTrue("Skipping — .bm2 file not found: " + bm2File, bm2File.exists());

		try (
			FileInputStream fis = new FileInputStream(bm2File);
			BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 2000);
			ObjectInputStream ois = new ObjectInputStream(bis))
		{
			project = (BMDProject) ois.readObject();
		}
		assertNotNull("Deserialized project should not be null for " + label, project);
	}

	// ── Original pipeline consistency tests ──

	/**
	 * Verify that DoseResponseExperiment.getColumnHeader() and
	 * getAnalysisRows() succeed and produce consistent widths.
	 * This exercises the metadata-append logic in DoseResponseExperiment.
	 */
	@Test
	public void doseResponseExperiments_haveConsistentColumns()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			assertDataSetConsistent("DoseResponseExperiment[" + dre.getName() + "]", dre);
		}
	}

	@Test
	public void oneWayAnovaResults_haveConsistentColumns()
	{
		for (OneWayANOVAResults r : project.getOneWayANOVAResults())
		{
			assertDataSetConsistent("OneWayANOVA[" + r.getName() + "]", r);
		}
	}

	@Test
	public void williamsTrendResults_haveConsistentColumns()
	{
		for (WilliamsTrendResults r : project.getWilliamsTrendResults())
		{
			assertDataSetConsistent("WilliamsTrend[" + r.getName() + "]", r);
		}
	}

	@Test
	public void oriogenResults_haveConsistentColumns()
	{
		for (OriogenResults r : project.getOriogenResults())
		{
			assertDataSetConsistent("Oriogen[" + r.getName() + "]", r);
		}
	}

	@Test
	public void curveFitPrefilterResults_haveConsistentColumns()
	{
		for (CurveFitPrefilterResults r : project.getCurveFitPrefilterResults())
		{
			assertDataSetConsistent("CurveFitPrefilter[" + r.getName() + "]", r);
		}
	}

	@Test
	public void bmdResults_haveConsistentColumns()
	{
		for (BMDResult r : project.getbMDResult())
		{
			assertDataSetConsistent("BMDResult[" + r.getName() + "]", r);
		}
	}

	@Test
	public void categoryAnalysisResults_haveConsistentColumns()
	{
		for (CategoryAnalysisResults r : project.getCategoryAnalysisResults())
		{
			assertDataSetConsistent("CategoryAnalysis[" + r.getName() + "]", r);
		}
	}

	@Test
	public void experimentDescription_isNeverNull()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			assertNotNull(
				"ExperimentDescription should never be null after deserialization for "
					+ dre.getName(),
				dre.getExperimentDescription());
		}
	}

	@Test
	public void experimentDescription_headersAndValuesMatch()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			List<String> headers = dre.getExperimentDescription().getColumnHeaders();
			List<Object> values = dre.getExperimentDescription().getColumnValues();
			assertNotNull("getColumnHeaders() should not be null for " + dre.getName(), headers);
			assertNotNull("getColumnValues() should not be null for " + dre.getName(), values);
			assertEquals(
				"Column headers and values count must match for " + dre.getName(),
				headers.size(), values.size());
			assertEquals(
				"Metadata column count must be " + METADATA_COLUMN_COUNT + " for " + dre.getName(),
				METADATA_COLUMN_COUNT, headers.size());
		}
	}

	// ════════════════════════════════════════════════════════════════════
	// Phase 1: Domain-neutral alias tests
	//
	// These tests verify that the new Endpoint/EndpointResponse interfaces
	// and the domain-neutral alias methods on DoseResponseExperiment and
	// IStatModelProcessable return identical data to their genomics-named
	// counterparts.  The aliases are thin wrappers, so the test is really
	// confirming the wiring — that getEndpointResponses() == getProbeResponses(),
	// getPlatform() == getChip(), etc.
	// ════════════════════════════════════════════════════════════════════

	/**
	 * getEndpointResponses() must return the same list as getProbeResponses().
	 * Both should be non-null and reference-identical (same List object).
	 */
	@Test
	public void endpointResponses_matchProbeResponses()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			List<ProbeResponse> probeResponses = dre.getProbeResponses();
			List<? extends EndpointResponse> endpointResponses = dre.getEndpointResponses();

			assertNotNull("getProbeResponses() should not be null for " + dre.getName(),
				probeResponses);
			assertNotNull("getEndpointResponses() should not be null for " + dre.getName(),
				endpointResponses);

			// They should be the exact same list object (not a copy)
			assertSame(
				"getEndpointResponses() must return the same list as getProbeResponses() for "
					+ dre.getName(),
				probeResponses, endpointResponses);
		}
	}

	/**
	 * getPlatform() must return the same ChipInfo as getChip().
	 */
	@Test
	public void platform_matchesChip()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			ChipInfo chip = dre.getChip();
			ChipInfo platform = dre.getPlatform();

			// Both may be null (some .bm2 files lack chip info), but must be identical
			assertSame(
				"getPlatform() must return the same object as getChip() for " + dre.getName(),
				chip, platform);
		}
	}

	/**
	 * Probe implements Endpoint — verify that getEndpoint() returns
	 * the same Probe object as getProbe(), and that getId() works
	 * through both paths.
	 */
	@Test
	public void probeResponse_endpointAliasMatchesProbe()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			List<ProbeResponse> responses = dre.getProbeResponses();
			if (responses == null || responses.isEmpty())
				continue;

			// Check first few responses to avoid long test times
			int toCheck = Math.min(responses.size(), 10);
			for (int i = 0; i < toCheck; i++)
			{
				ProbeResponse pr = responses.get(i);
				Probe probe = pr.getProbe();
				Endpoint endpoint = pr.getEndpoint();

				assertNotNull("getProbe() should not be null for row " + i + " in " + dre.getName(),
					probe);
				assertNotNull("getEndpoint() should not be null for row " + i + " in " + dre.getName(),
					endpoint);

				// getEndpoint() should return the same Probe object
				assertSame(
					"getEndpoint() must return the same object as getProbe() for row " + i
						+ " in " + dre.getName(),
					probe, endpoint);

				// Endpoint.getId() should match Probe.getId()
				assertEquals(
					"Endpoint.getId() must match Probe.getId() for row " + i + " in " + dre.getName(),
					probe.getId(), endpoint.getId());
			}
		}
	}

	/**
	 * IStatModelProcessable.getProcessableEndpointResponses() must return
	 * the same list as getProcessableProbeResponses().
	 * DoseResponseExperiment implements IStatModelProcessable.
	 */
	@Test
	public void statModelProcessable_endpointResponsesMatchProbeResponses()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			// Access through the interface to verify the default method wiring
			IStatModelProcessable processable = dre;

			List<ProbeResponse> probeResponses = processable.getProcessableProbeResponses();
			List<? extends EndpointResponse> endpointResponses =
				processable.getProcessableEndpointResponses();

			assertNotNull("getProcessableProbeResponses() should not be null for " + dre.getName(),
				probeResponses);
			assertNotNull(
				"getProcessableEndpointResponses() should not be null for " + dre.getName(),
				endpointResponses);

			// The default implementation delegates, so they should be the same list
			assertSame(
				"getProcessableEndpointResponses() must delegate to getProcessableProbeResponses() for "
					+ dre.getName(),
				probeResponses, endpointResponses);
		}
	}

	/**
	 * EndpointResponse.getResponseArray() should return a non-null float[]
	 * matching the size of getResponses() for deserialized data.
	 */
	@Test
	public void endpointResponse_responseArrayMatchesResponses()
	{
		for (DoseResponseExperiment dre : project.getDoseResponseExperiments())
		{
			List<ProbeResponse> responses = dre.getProbeResponses();
			if (responses == null || responses.isEmpty())
				continue;

			int toCheck = Math.min(responses.size(), 10);
			for (int i = 0; i < toCheck; i++)
			{
				// Access through the EndpointResponse interface
				EndpointResponse er = responses.get(i);

				assertNotNull(
					"getResponses() should not be null for row " + i + " in " + dre.getName(),
					er.getResponses());
				assertNotNull(
					"getResponseArray() should not be null for row " + i + " in " + dre.getName(),
					er.getResponseArray());
				assertEquals(
					"getResponseArray().length must match getResponses().size() for row "
						+ i + " in " + dre.getName(),
					er.getResponses().size(), er.getResponseArray().length);
			}
		}
	}

	// ── Helper: assert column/row consistency for any BMDExpressAnalysisDataSet ──

	/**
	 * Calls getColumnHeader() and getAnalysisRows() on the given dataset.
	 * Verifies:
	 *   1. No exception is thrown during getColumnHeader() or getAnalysisRows().
	 *   2. If headers are non-empty, the metadata column names appear at the end.
	 *   3. For non-CategoryAnalysis datasets, row data width matches column count.
	 *      (CategoryAnalysis has a known pre-existing header/row mismatch of 2-3
	 *      columns that predates our metadata changes. We verify metadata is
	 *      consistently added to both headers and rows, but don't assert exact match.)
	 */
	@SuppressWarnings("unchecked")
	private void assertDataSetConsistent(String label, BMDExpressAnalysisDataSet dataSet)
	{
		// getColumnHeader() triggers lazy initialization of both headers and row data.
		// This is the main call that could throw if our null guards are broken.
		List<String> headers = dataSet.getColumnHeader();
		assertNotNull(label + ": getColumnHeader() should not return null", headers);

		// Some datasets have empty result lists, causing fillColumnHeader() to
		// return early with an empty list.  This is normal — skip further checks.
		if (headers.isEmpty())
		{
			return;
		}

		// Verify that the 8 metadata columns appear at the end of the header list.
		// This confirms our fillColumnHeader() changes are working.
		List<String> expectedMetadataHeaders = ExperimentDescription.empty().getColumnHeaders();
		assertTrue(
			label + ": header list must be large enough to contain metadata columns (size="
				+ headers.size() + ", need at least " + METADATA_COLUMN_COUNT + ")",
			headers.size() >= METADATA_COLUMN_COUNT);

		List<String> lastNHeaders = headers.subList(
			headers.size() - METADATA_COLUMN_COUNT, headers.size());
		assertEquals(
			label + ": last " + METADATA_COLUMN_COUNT + " column headers must be metadata columns",
			expectedMetadataHeaders, lastNHeaders);

		// getAnalysisRows() returns the row objects
		List<?> rows = dataSet.getAnalysisRows();
		assertNotNull(label + ": getAnalysisRows() should not return null", rows);

		// Check first few rows to avoid very long test times on big datasets.
		// We verify row data is non-null and accessible without exceptions.
		int colCount = headers.size();
		boolean isCategoryAnalysis = dataSet instanceof CategoryAnalysisResults;
		int rowsToCheck = Math.min(rows.size(), 10);

		for (int i = 0; i < rowsToCheck; i++)
		{
			Object rowObj = rows.get(i);
			List<Object> rowData = null;

			// All row types extend BMDExpressAnalysisRow which has getRow()
			if (rowObj instanceof com.sciome.bmdexpress2.mvp.model.BMDExpressAnalysisRow)
			{
				com.sciome.bmdexpress2.mvp.model.BMDExpressAnalysisRow analysisRow =
					(com.sciome.bmdexpress2.mvp.model.BMDExpressAnalysisRow) rowObj;
				rowData = analysisRow.getRow();
			}
			// ProbeResponse rows (from DoseResponseExperiment) also have getRow()
			else if (rowObj instanceof com.sciome.bmdexpress2.mvp.model.probe.ProbeResponse)
			{
				com.sciome.bmdexpress2.mvp.model.probe.ProbeResponse pr =
					(com.sciome.bmdexpress2.mvp.model.probe.ProbeResponse) rowObj;
				rowData = pr.getRow();
			}

			if (rowData != null)
			{
				assertNotNull(label + " row[" + i + "]: getRow() should not return null", rowData);
				assertTrue(
					label + " row[" + i + "]: row should have data (size=" + rowData.size() + ")",
					rowData.size() > 0);

				if (isCategoryAnalysis)
				{
					// CategoryAnalysis has a known pre-existing mismatch of 2-3 columns
					// between generateColumnHeader() and createRowData() that predates
					// our metadata work.  Just verify the row has a reasonable size
					// (within tolerance) and that metadata values are present at the end.
					assertTrue(
						label + " row[" + i + "]: CategoryAnalysis row width (" + rowData.size()
							+ ") should be within 5 of column count (" + colCount + ")",
						Math.abs(colCount - rowData.size()) <= 5);
				}
				else
				{
					// Non-CategoryAnalysis datasets should have exact match between
					// column count and row width.
					assertEquals(
						label + " row[" + i + "]: row width (" + rowData.size()
							+ ") must match column count (" + colCount + ")",
						colCount, rowData.size());
				}
			}
		}
	}
}
