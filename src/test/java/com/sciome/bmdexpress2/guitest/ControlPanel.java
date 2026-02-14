package com.sciome.bmdexpress2.guitest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sciome.bmdexpress2.guitest.model.StepDefinition;
import com.sciome.bmdexpress2.guitest.model.WorkflowDefinition;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ControlPanel
{
	private final ActionExecutor executor;
	private final WorkflowLoader loader;
	private final CountDownLatch closeLatch = new CountDownLatch(1);

	private Stage stage;
	private ComboBox<WorkflowDefinition> workflowCombo;
	private ListView<String> stepListView;
	private Label statusLabel;
	private Label resultLabel;
	private Slider speedSlider;
	private Button nextButton;
	private Button playButton;
	private Button pauseButton;
	private Button resetButton;

	private WorkflowDefinition currentWorkflow;
	private int currentStep = -1;
	private final AtomicBoolean playing = new AtomicBoolean(false);
	private Thread playThread;

	public ControlPanel(ActionExecutor executor)
	{
		this.executor = executor;
		this.loader = new WorkflowLoader();
	}

	public void show()
	{
		stage = new Stage();
		stage.initStyle(StageStyle.UTILITY);
		stage.setAlwaysOnTop(true);
		stage.setTitle("Visual Test Runner");

		VBox root = new VBox(8);
		root.setPadding(new Insets(10));
		root.setAlignment(Pos.TOP_LEFT);

		// Workflow selector
		Label workflowLabel = new Label("Workflow:");
		workflowCombo = new ComboBox<>();
		workflowCombo.setMaxWidth(Double.MAX_VALUE);
		loadWorkflows();
		workflowCombo.setOnAction(e -> onWorkflowSelected());

		// Step list
		Label stepsLabel = new Label("Steps:");
		stepListView = new ListView<>();
		stepListView.setPrefHeight(250);
		stepListView.setCellFactory(lv -> new ListCell<String>()
		{
			@Override
			protected void updateItem(String item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null)
				{
					setText(null);
					setStyle("");
				}
				else
				{
					setText(item);
					int idx = getIndex();
					if (idx == currentStep)
					{
						setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
					}
					else if (idx < currentStep)
					{
						setStyle("-fx-text-fill: #888;");
					}
					else
					{
						setStyle("");
					}
				}
			}
		});

		// Status
		statusLabel = new Label("Select a workflow to begin");
		statusLabel.setStyle("-fx-font-size: 11px;");
		resultLabel = new Label("");
		resultLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

		// Speed slider
		HBox speedBox = new HBox(8);
		speedBox.setAlignment(Pos.CENTER_LEFT);
		Label speedLabel = new Label("Delay:");
		speedSlider = new Slider(100, 3000, 1000);
		speedSlider.setShowTickLabels(true);
		speedSlider.setShowTickMarks(true);
		speedSlider.setMajorTickUnit(500);
		speedSlider.setBlockIncrement(100);
		HBox.setHgrow(speedSlider, Priority.ALWAYS);
		Label speedValueLabel = new Label("1000ms");
		speedSlider.valueProperty().addListener((obs, old, val) ->
				speedValueLabel.setText(val.intValue() + "ms"));
		speedBox.getChildren().addAll(speedLabel, speedSlider, speedValueLabel);

		// Buttons
		HBox buttonBox = new HBox(8);
		buttonBox.setAlignment(Pos.CENTER);
		nextButton = new Button("Next");
		playButton = new Button("Play");
		pauseButton = new Button("Pause");
		resetButton = new Button("Reset");

		nextButton.setDisable(true);
		playButton.setDisable(true);
		pauseButton.setDisable(true);
		resetButton.setDisable(true);

		nextButton.setOnAction(e -> onNext());
		playButton.setOnAction(e -> onPlay());
		pauseButton.setOnAction(e -> onPause());
		resetButton.setOnAction(e -> onReset());

		buttonBox.getChildren().addAll(nextButton, playButton, pauseButton, resetButton);

		root.getChildren().addAll(
				workflowLabel, workflowCombo,
				stepsLabel, stepListView,
				statusLabel, resultLabel,
				speedBox, buttonBox);
		VBox.setVgrow(stepListView, Priority.ALWAYS);

		Scene scene = new Scene(root, 380, 520);
		stage.setScene(scene);
		stage.setOnCloseRequest(e -> closeLatch.countDown());
		stage.show();
	}

	public void awaitClose() throws InterruptedException
	{
		closeLatch.await();
	}

	private void loadWorkflows()
	{
		try
		{
			List<WorkflowDefinition> workflows = loader.loadAll();
			workflowCombo.setItems(FXCollections.observableArrayList(workflows));
		}
		catch (IOException e)
		{
			statusLabel.setText("Error loading workflows: " + e.getMessage());
		}
	}

	private void onWorkflowSelected()
	{
		currentWorkflow = workflowCombo.getValue();
		if (currentWorkflow == null) return;

		currentStep = -1;
		ObservableList<String> items = FXCollections.observableArrayList();
		List<StepDefinition> steps = currentWorkflow.getSteps();
		for (int i = 0; i < steps.size(); i++)
		{
			StepDefinition step = steps.get(i);
			String desc = step.getDescription() != null ? step.getDescription() : step.getAction();
			items.add((i + 1) + ". [" + step.getAction() + "] " + desc);
		}
		stepListView.setItems(items);

		statusLabel.setText("Ready: " + currentWorkflow.getName()
				+ " (" + steps.size() + " steps)");
		resultLabel.setText("");
		nextButton.setDisable(false);
		playButton.setDisable(false);
		resetButton.setDisable(false);
		pauseButton.setDisable(true);
	}

	private void onNext()
	{
		if (currentWorkflow == null) return;

		currentStep++;
		List<StepDefinition> steps = currentWorkflow.getSteps();
		if (currentStep >= steps.size())
		{
			statusLabel.setText("Workflow complete!");
			nextButton.setDisable(true);
			playButton.setDisable(true);
			currentStep = steps.size();
			refreshList();
			return;
		}

		StepDefinition step = steps.get(currentStep);
		statusLabel.setText("Step " + (currentStep + 1) + "/" + steps.size()
				+ " — " + step.getAction());
		refreshList();
		stepListView.scrollTo(currentStep);

		String result = executor.execute(step);
		resultLabel.setText(result);

		if (currentStep + 1 >= steps.size())
		{
			statusLabel.setText("Workflow complete!");
			nextButton.setDisable(true);
			playButton.setDisable(true);
		}
	}

	private void onPlay()
	{
		if (currentWorkflow == null) return;
		playing.set(true);
		playButton.setDisable(true);
		nextButton.setDisable(true);
		pauseButton.setDisable(false);

		playThread = new Thread(() ->
		{
			List<StepDefinition> steps = currentWorkflow.getSteps();
			while (playing.get())
			{
				int nextIdx = currentStep + 1;
				if (nextIdx >= steps.size())
				{
					playing.set(false);
					Platform.runLater(() ->
					{
						statusLabel.setText("Workflow complete!");
						playButton.setDisable(true);
						nextButton.setDisable(true);
						pauseButton.setDisable(true);
					});
					break;
				}

				Platform.runLater(this::onNext);

				try
				{
					Thread.sleep((long) speedSlider.getValue());
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					break;
				}
			}
		}, "visual-test-autoplay");
		playThread.setDaemon(true);
		playThread.start();
	}

	private void onPause()
	{
		playing.set(false);
		pauseButton.setDisable(true);
		playButton.setDisable(false);
		nextButton.setDisable(currentStep + 1 >= currentWorkflow.getSteps().size());
	}

	private void onReset()
	{
		playing.set(false);
		currentStep = -1;
		refreshList();
		statusLabel.setText("Reset: " + currentWorkflow.getName());
		resultLabel.setText("");
		nextButton.setDisable(false);
		playButton.setDisable(false);
		pauseButton.setDisable(true);
	}

	private void refreshList()
	{
		// Force cell refresh to update the current-step marker
		ObservableList<String> items = stepListView.getItems();
		stepListView.setItems(null);
		stepListView.setItems(items);
	}
}
