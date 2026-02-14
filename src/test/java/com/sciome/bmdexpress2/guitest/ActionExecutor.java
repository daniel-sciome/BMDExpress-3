package com.sciome.bmdexpress2.guitest;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import com.sciome.bmdexpress2.guitest.model.StepDefinition;

import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.input.KeyCode;
import javafx.stage.Window;

public class ActionExecutor
{
	private final FxRobot robot;

	public ActionExecutor(FxRobot robot)
	{
		this.robot = robot;
	}

	public String execute(StepDefinition step)
	{
		String action = step.getAction();
		try
		{
			switch (action)
			{
				case "click":
					doClick(step);
					break;
				case "click_menu":
					doClickMenu(step);
					break;
				case "type":
					doType(step);
					break;
				case "write":
					doWrite(step);
					break;
				case "select_combo":
					doSelectCombo(step);
					break;
				case "click_dialog_button":
					doClickDialogButton(step);
					break;
				case "stub_file_chooser":
					doStubFileChooser(step);
					break;
				case "clear_file_chooser_stub":
					doClearFileChooserStub(step);
					break;
				case "wait":
					doWait(step);
					break;
				case "wait_for":
					doWaitFor(step);
					break;
				case "press_key":
					doPressKey(step);
					break;
				case "assert_visible":
					return doAssertVisible(step);
				case "assert_text":
					return doAssertText(step);
				case "comment":
					return "OK";
				default:
					return "UNKNOWN ACTION: " + action;
			}
			WaitForAsyncUtils.waitForFxEvents();
			return "OK";
		}
		catch (Exception e)
		{
			return "ERROR: " + e.getMessage();
		}
	}

	private void doClick(StepDefinition step)
	{
		robot.clickOn(step.getTarget());
	}

	private void doClickMenu(StepDefinition step)
	{
		for (String item : step.getPath())
		{
			robot.clickOn(item);
			WaitForAsyncUtils.waitForFxEvents();
		}
	}

	private void doType(StepDefinition step)
	{
		robot.clickOn(step.getTarget());
		robot.eraseText(100);
		robot.write(step.getText());
	}

	private void doWrite(StepDefinition step)
	{
		robot.write(step.getText());
	}

	private void doSelectCombo(StepDefinition step)
	{
		robot.clickOn(step.getTarget());
		WaitForAsyncUtils.waitForFxEvents();
		robot.clickOn(step.getValue());
	}

	private void doClickDialogButton(StepDefinition step)
	{
		robot.clickOn(step.getText());
	}

	@SuppressWarnings("unchecked")
	private void doStubFileChooser(StepDefinition step) throws Exception
	{
		List<File> files = step.getFiles().stream()
				.map(File::new)
				.collect(Collectors.toList());

		Class<?> menuBarViewClass = Class.forName(
				"com.sciome.bmdexpress2.mvp.view.mainstage.MenuBarView");
		Field field = menuBarViewClass.getDeclaredField("fileChooserSupplier");
		field.setAccessible(true);

		BiFunction<Window, String, List<File>> supplier = (window, title) -> files;
		field.set(null, supplier);
	}

	private void doClearFileChooserStub(StepDefinition step) throws Exception
	{
		Class<?> menuBarViewClass = Class.forName(
				"com.sciome.bmdexpress2.mvp.view.mainstage.MenuBarView");
		Field field = menuBarViewClass.getDeclaredField("fileChooserSupplier");
		field.setAccessible(true);
		field.set(null, null);
	}

	private void doWait(StepDefinition step) throws InterruptedException
	{
		long millis = step.getMillis() != null ? step.getMillis() : 1000;
		Thread.sleep(millis);
	}

	private void doWaitFor(StepDefinition step) throws Exception
	{
		long timeout = step.getTimeoutMillis() != null ? step.getTimeoutMillis() : 5000;
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeout)
		{
			Optional<Node> node = robot.lookup(step.getTarget()).tryQuery();
			if (node.isPresent() && node.get().isVisible())
			{
				return;
			}
			Thread.sleep(200);
		}
		throw new RuntimeException("Timeout waiting for: " + step.getTarget());
	}

	private void doPressKey(StepDefinition step)
	{
		KeyCode keyCode = KeyCode.valueOf(step.getKey());
		robot.press(keyCode);
		robot.release(keyCode);
	}

	private String doAssertVisible(StepDefinition step)
	{
		Optional<Node> node = robot.lookup(step.getTarget()).tryQuery();
		if (node.isPresent() && node.get().isVisible())
		{
			return "PASS: visible - " + step.getTarget();
		}
		return "FAIL: not visible - " + step.getTarget();
	}

	private String doAssertText(StepDefinition step)
	{
		Optional<Node> node = robot.lookup(step.getTarget()).tryQuery();
		if (node.isPresent() && node.get() instanceof Labeled)
		{
			String actual = ((Labeled) node.get()).getText();
			if (step.getText().equals(actual))
			{
				return "PASS: text matches - " + step.getText();
			}
			return "FAIL: expected '" + step.getText() + "' but got '" + actual + "'";
		}
		return "FAIL: node not found or not Labeled - " + step.getTarget();
	}
}
