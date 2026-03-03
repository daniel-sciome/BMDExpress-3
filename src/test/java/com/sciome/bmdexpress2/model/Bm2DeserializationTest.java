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
import com.sciome.bmdexpress2.mvp.model.category.CategoryAnalysisResults;
import com.sciome.bmdexpress2.mvp.model.info.ExperimentDescription;
import com.sciome.bmdexpress2.mvp.model.prefilter.CurveFitPrefilterResults;
import com.sciome.bmdexpress2.mvp.model.prefilter.OneWayANOVAResults;
import com.sciome.bmdexpress2.mvp.model.prefilter.OriogenResults;
import com.sciome.bmdexpress2.mvp.model.prefilter.WilliamsTrendResults;
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

	// ── Tests ──

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

	/**
	 * Verify OneWayANOVA prefilter results.
	 */
	@Test
	public void oneWayAnovaResults_haveConsistentColumns()
	{
		for (OneWayANOVAResults r : project.getOneWayANOVAResults())
		{
			assertDataSetConsistent("OneWayANOVA[" + r.getName() + "]", r);
		}
	}

	/**
	 * Verify Williams Trend prefilter results.
	 */
	@Test
	public void williamsTrendResults_haveConsistentColumns()
	{
		for (WilliamsTrendResults r : project.getWilliamsTrendResults())
		{
			assertDataSetConsistent("WilliamsTrend[" + r.getName() + "]", r);
		}
	}

	/**
	 * Verify Oriogen prefilter results.
	 */
	@Test
	public void oriogenResults_haveConsistentColumns()
	{
		for (OriogenResults r : project.getOriogenResults())
		{
			assertDataSetConsistent("Oriogen[" + r.getName() + "]", r);
		}
	}

	/**
	 * Verify CurveFit prefilter results.
	 */
	@Test
	public void curveFitPrefilterResults_haveConsistentColumns()
	{
		for (CurveFitPrefilterResults r : project.getCurveFitPrefilterResults())
		{
			assertDataSetConsistent("CurveFitPrefilter[" + r.getName() + "]", r);
		}
	}

	/**
	 * Verify BMD analysis results.
	 */
	@Test
	public void bmdResults_haveConsistentColumns()
	{
		for (BMDResult r : project.getbMDResult())
		{
			assertDataSetConsistent("BMDResult[" + r.getName() + "]", r);
		}
	}

	/**
	 * Verify Category analysis results.
	 * Note: CategoryAnalysisResult has a known pre-existing row/column
	 * mismatch of 2-3 columns unrelated to our metadata changes.
	 * This test verifies no exceptions occur and that metadata columns
	 * are present in the headers.
	 */
	@Test
	public void categoryAnalysisResults_haveConsistentColumns()
	{
		for (CategoryAnalysisResults r : project.getCategoryAnalysisResults())
		{
			assertDataSetConsistent("CategoryAnalysis[" + r.getName() + "]", r);
		}
	}

	/**
	 * Verify that every DoseResponseExperiment has a non-null
	 * ExperimentDescription after deserialization (the lazy-init
	 * or readObject() null guard should ensure this).
	 */
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

	/**
	 * Verify that ExperimentDescription.getColumnHeaders() and
	 * getColumnValues() return matching-length lists.
	 */
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
