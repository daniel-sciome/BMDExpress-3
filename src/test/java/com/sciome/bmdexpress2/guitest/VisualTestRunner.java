package com.sciome.bmdexpress2.guitest;

import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import com.sciome.bmdexpress2.BMDExpress3Main;

import javafx.stage.Stage;

public class VisualTestRunner extends ApplicationTest
{
	private ControlPanel controlPanel;

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		// Launch the real BMDExpress application
		new BMDExpress3Main().start(primaryStage);

		// Create floating control panel
		ActionExecutor executor = new ActionExecutor(this); // ApplicationTest IS a FxRobot
		controlPanel = new ControlPanel(executor);
		controlPanel.show();
	}

	@Test
	public void runVisualTests() throws Exception
	{
		controlPanel.awaitClose(); // blocks until user closes panel
	}
}
