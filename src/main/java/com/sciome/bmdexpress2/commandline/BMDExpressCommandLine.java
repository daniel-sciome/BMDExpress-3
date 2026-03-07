package com.sciome.bmdexpress2.commandline;

/**
 * bmdx-core CLI entry point — stripped of analysis runners (BMD, ANOVA,
 * category, etc.) which depend on the full modeling engine.
 *
 * Retained commands:
 *   export   — export .bm2 data to tabular text or JSON
 *   combine  — merge multiple .bm2 files into one
 *   query    — list analysis groups/names in a .bm2
 *   delete   — remove an analysis from a .bm2
 *   --version
 *
 * Removed commands (require full BMDExpress engine):
 *   analyze  — run prefilter → BMD → category pipeline
 */

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.sciome.bmdexpress2.shared.BMDExpressProperties;

public class BMDExpressCommandLine
{

	public final static String INPUT_BM2 = "input-bm2";
	public final static String INPUT_BM2_FILES = "input-bm2-files";
	public final static String ANALYSIS_GROUP = "analysis-group";
	public final static String ANALYSIS_NAME = "analysis-name";
	public final static String OUTPUT_FILE_NAME = "output-file-name";

	public final static String EXPORT = "export";
	public final static String DELETE = "delete";
	public final static String COMBINE = "combine";
	public final static String QUERY = "query";
	public final static String VERSION = "--version";
	public final static String VERY_VERBOSE = "veryverbose";

	// Analysis group names — used by export, query, and delete commands
	public final static String EXPRESSION = "expression";
	public final static String ONE_WAY_ANOVA = "anova";
	public final static String WILLIAMS = "williams";
	public final static String CURVE_FIT_PREFILTER = "curvefit_prefilter";
	public final static String ORIOGEN = "oriogen";
	public final static String BMD_ANALYSIS = "bmd";
	public final static String CATEGORICAL = "categorical";

	Options exportOptions = new Options();
	Options deleteOptions = new Options();
	Options queryOptions = new Options();
	Options combineOptions = new Options();

	public static void main(String[] args)
	{
		new BMDExpressCommandLine().run(args);
	}

	private void run(String[] args)
	{
		CommandLineParser parser = new DefaultParser();

		exportOptions.addOption(
				Option.builder().longOpt(INPUT_BM2).hasArg().required().argName("BM2FILE").build());
		exportOptions.addOption(
				Option.builder().longOpt(ANALYSIS_GROUP).hasArg().required().argName("GROUP").build());
		exportOptions.addOption(Option.builder().longOpt(ANALYSIS_NAME).hasArg().argName("NAME").build());
		exportOptions.addOption(
				Option.builder().longOpt(OUTPUT_FILE_NAME).hasArg().required().argName("OUTPUT").build());
		exportOptions.addOption(Option.builder().longOpt(VERY_VERBOSE).hasArg(false).required(false).build());

		deleteOptions.addOption(
				Option.builder().longOpt(INPUT_BM2).hasArg().required().argName("BM2FILE").build());
		deleteOptions.addOption(
				Option.builder().longOpt(ANALYSIS_GROUP).hasArg().required().argName("GROUP").build());
		deleteOptions.addOption(
				Option.builder().longOpt(ANALYSIS_NAME).hasArg().required().argName("NAME").build());
		deleteOptions.addOption(Option.builder().longOpt(VERY_VERBOSE).hasArg(false).required(false).build());

		queryOptions.addOption(
				Option.builder().longOpt(INPUT_BM2).hasArg().required().argName("BM2FILE").build());
		queryOptions.addOption(
				Option.builder().longOpt(ANALYSIS_GROUP).hasArg().required().argName("GROUP").build());
		queryOptions.addOption(Option.builder().longOpt(VERY_VERBOSE).hasArg(false).required(false).build());

		combineOptions.addOption(
				Option.builder().longOpt(OUTPUT_FILE_NAME).hasArg().required().argName("OUTPUT").build());
		combineOptions.addOption(Option.builder().longOpt(INPUT_BM2_FILES).hasArgs().required()
				.argName("INPUT BM2 FILES").build());
		combineOptions
				.addOption(Option.builder().longOpt(VERY_VERBOSE).hasArg(false).required(false).build());

		try
		{
			if (args.length < 1)
			{
				printHelp();
				return;
			}
			String[] theArgs = Arrays.copyOfRange(args, 1, args.length);
			if (args[0].equals(EXPORT))
			{
				CommandLine cmd = parser.parse(exportOptions, theArgs);
				ExportRunner eRunner = new ExportRunner();
				eRunner.analyze(cmd.getOptionValue(INPUT_BM2), cmd.getOptionValue(OUTPUT_FILE_NAME),
						cmd.getOptionValue(ANALYSIS_GROUP), cmd.getOptionValue(ANALYSIS_NAME));
			}
			else if (args[0].equals(DELETE))
			{
				CommandLine cmd = parser.parse(deleteOptions, theArgs);
				DeleteRunner dRunner = new DeleteRunner();
				dRunner.analyze(cmd.getOptionValue(INPUT_BM2), cmd.getOptionValue(ANALYSIS_GROUP),
						cmd.getOptionValue(ANALYSIS_NAME));
			}
			else if (args[0].equals(QUERY))
			{
				CommandLine cmd = parser.parse(queryOptions, theArgs);
				QueryRunner qRunner = new QueryRunner();
				qRunner.analyze(cmd.getOptionValue(INPUT_BM2), cmd.getOptionValue(ANALYSIS_GROUP));
			}
			else if (args[0].equals(COMBINE))
			{
				CommandLine cmd = parser.parse(combineOptions, theArgs);
				CombineRunner cRunner = new CombineRunner();
				cRunner.combine(cmd.getOptionValue(OUTPUT_FILE_NAME),
						Arrays.asList(cmd.getOptionValues(INPUT_BM2_FILES)));
			}
			else if (args[0].equals(VERSION))
			{
				System.out.println(BMDExpressProperties.getInstance().getVersion());
			}
			else
			{
				System.err.println("Unknown command: " + args[0]);
				printHelp();
			}
		}
		catch (Exception exp)
		{
			exp.printStackTrace();
			System.out.println("Error:" + exp.getMessage());
			printHelp();
		}
	}

	private void printHelp()
	{
		HelpFormatter formatter = new HelpFormatter();

		formatter.setWidth(160);
		System.out.println("usage: bmdx-core " + VERSION);
		formatter.printHelp("bmdx-core " + EXPORT, "", exportOptions, "", true);
		formatter.printHelp("bmdx-core " + DELETE, "", deleteOptions, "", true);
		formatter.printHelp("bmdx-core " + QUERY, "", queryOptions, "", true);
		formatter.printHelp("bmdx-core " + COMBINE, "", combineOptions, "", true);

		System.out.println("<GROUP>: " + EXPRESSION + ", " + ONE_WAY_ANOVA + ", " + WILLIAMS + ", "
				+ CURVE_FIT_PREFILTER + ", " + ORIOGEN + ", " + BMD_ANALYSIS + ", " + CATEGORICAL);
	}
}
