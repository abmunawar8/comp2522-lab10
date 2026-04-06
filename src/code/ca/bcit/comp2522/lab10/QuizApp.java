package ca.bcit.comp2522.lab10;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Provides a graphical user interface for a randomized general knowledge quiz.
 *
 * @author Claire Li        Set D
 * @author Abdullah Munawar Set D
 * 
 * @version 1.0
 */
public class QuizApp extends Application
{
    private static final int SCENE_WIDTH_PX         = 600;
    private static final int SCENE_HEIGHT_PX        = 450;
    private static final int TOTAL_QUESTIONS_COUNT  = 10;
    private static final int VBOX_SPACING_PX        = 15;
    private static final int PADDING_PX             = 20;
    private static final int SUMMARY_AREA_HEIGHT_PX = 150;
    private static final int FIRST_QUESTION_NUMBER  = 1;
    private static final int BASE_SCORE             = 0;
    private static final int THREAD_SLEEP_MS        = 1000;
    private static final int EXACT_QUESTION_PARTS   = 2;
    private static final int INDEX_FOR_QUESTION     = 0;
    private static final int INDEX_FOR_ANSWER       = 1;

    private final Random        randomGenerator;
    private final List<String>  missedQuestions;
    private final List<String>  missedAnswers;
    private final List<Integer> questionIndices;
    private final List<Integer> incorrectQuestions;

    private String       currentQuestion;
    private String       currentAnswer;
    private Scene        quizScene;
    private Label        questionLabel;
    private Button       submitButton;
    private Button       restartButton;
    private TextField    answerTextField;
    private Label        scoreLabel;
    private TextArea     summaryTextArea;
    private int          currentScore;
    private int          currentQuestionNumber;
    private List<String> quizLines;

    /**
     * Constructs a new QuizApp and initializes internal collections.
     */
    public QuizApp()
    {
        randomGenerator    = new Random();
        missedQuestions    = new ArrayList<>();
        missedAnswers      = new ArrayList<>();
        questionIndices    = new ArrayList<>();
        incorrectQuestions = new ArrayList<>();
    }

    /**
     * Initializes and displays the primary stage for the JavaFX application.
     * @param primaryStage the top-level container for this application
     */
    @Override
    public final void start(final Stage primaryStage)
    {
        final URL       cssFileUrl;
        final VBox      startLayout;
        final VBox      quizLayout;
        final Label     welcomeLabel;
        final Scene     startScene;
        final Button    startButton;
        final StackPane buttonStackPane;

        cssFileUrl = getClass().getResource("/styles.css");
        if (cssFileUrl == null)
        {
            throw new RuntimeException("Unable to load styles.css");
        }

        welcomeLabel = new Label("Loading quiz data...");
        startButton  = new Button("Start Quiz");
        startButton.setDisable(true);

        final Task<List<String>> loadQuizTask;
        loadQuizTask = new Task<>()
        {
            @Override
            protected List<String> call() throws Exception
            {
                final URL quizFileUrl;
                quizFileUrl = getClass().getResource("/quiz.txt");
                if (quizFileUrl == null)
                {
                    throw new RuntimeException("Unable to load Quiz File");
                }
                Thread.sleep(THREAD_SLEEP_MS);
                return Files.readAllLines(Paths.get(quizFileUrl.toURI()));
            }
        };

        loadQuizTask.setOnSucceeded(event ->
        {
            quizLines = loadQuizTask.getValue();
            welcomeLabel.setText("Welcome to the Quiz App!");
            startButton.setDisable(false);
        });

        new Thread(loadQuizTask).start();

        currentScore          = BASE_SCORE;
        currentQuestionNumber = FIRST_QUESTION_NUMBER;

        startLayout = new VBox(VBOX_SPACING_PX);
        startScene  = new Scene(startLayout, SCENE_WIDTH_PX, SCENE_HEIGHT_PX);
        
        startLayout.setPadding(new Insets(PADDING_PX));
        startLayout.getChildren().addAll(welcomeLabel, startButton);
        startLayout.getStyleClass().add("root-box");
        startScene.getStylesheets().add(cssFileUrl.toExternalForm());

        scoreLabel      = new Label("Score: " + currentScore);
        questionLabel   = new Label("Question 1 of " + TOTAL_QUESTIONS_COUNT + " : ");
        answerTextField = new TextField();
        summaryTextArea = new TextArea();
        submitButton    = new Button("Submit");
        restartButton   = new Button("Restart");
        buttonStackPane = new StackPane();
        quizLayout      = new VBox(VBOX_SPACING_PX);
        quizScene       = new Scene(quizLayout, SCENE_WIDTH_PX, SCENE_HEIGHT_PX);
        
        questionLabel.getStyleClass().add("question-label");
        summaryTextArea.setEditable(false);
        summaryTextArea.setWrapText(true);
        summaryTextArea.setVisible(false);
        summaryTextArea.setPrefHeight(SUMMARY_AREA_HEIGHT_PX);
        restartButton.setVisible(false);
        buttonStackPane.getChildren().addAll(submitButton, restartButton);
        
        quizLayout.setPadding(new Insets(PADDING_PX));
        quizLayout.getChildren().addAll(scoreLabel, questionLabel, answerTextField, 
                                        summaryTextArea, buttonStackPane);
        quizLayout.getStyleClass().add("root-box");
        quizScene.getStylesheets().add(cssFileUrl.toExternalForm());

        startButton.setOnAction(event ->
        {
            final StringBuilder questionBuilder;
            questionBuilder = new StringBuilder();

            generateQuestionIndices();
            getNextQuestion();
            
            // Applied StringBuilder for dynamic label setup
            questionBuilder.append("Question ")
                           .append(currentQuestionNumber)
                           .append(" of ")
                           .append(TOTAL_QUESTIONS_COUNT)
                           .append(" : ")
                           .append(currentQuestion);
            
            questionLabel.setText(questionBuilder.toString());
            primaryStage.setScene(quizScene);
            missedQuestions.clear();
            missedAnswers.clear();
        });

        submitButton.setOnAction(event -> submitUserAnswer());
        answerTextField.setOnAction(event -> submitUserAnswer());
        restartButton.setOnAction(event -> restartQuiz());

        primaryStage.setScene(startScene);
        primaryStage.setTitle("Lab10 Quiz App");
        primaryStage.show();
    }

    /*
     * Populates the session list with 10 unique, random indices from the file.
     */
    private final void generateQuestionIndices()
    {
        questionIndices.clear();
        while (questionIndices.size() < TOTAL_QUESTIONS_COUNT)
        {
            final int randomIndex;
            randomIndex = randomGenerator.nextInt(quizLines.size());
            
            if (!questionIndices.contains(randomIndex))
            {
                questionIndices.add(randomIndex);
            }
        }
    }

    /*
     * Selects a random question and splits the line into question and answer.
     */
    private final void getNextQuestion()
    {
        final int      index;
        final String   line;
        final String[] questionParts;

        if (quizLines == null ||
            quizLines.isEmpty())
        {
            throw new RuntimeException("Quiz file is empty or not loaded");
        }

        index = questionIndices.get(currentQuestionNumber - 1);
        line  = quizLines.get(index);
        questionParts = line.split("\\|");
        
        if (questionParts.length != EXACT_QUESTION_PARTS)
        {
            throw new RuntimeException("Invalid quiz line format");
        }
        
        currentQuestion = questionParts[INDEX_FOR_QUESTION];
        currentAnswer   = questionParts[INDEX_FOR_ANSWER];
    }

    /*
     * Retrieves user input and checks it against the current answer.
     */
    private final void submitUserAnswer()
    {
        final String userInput;
        userInput = answerTextField.getText();
        
        evaluateUserAnswer(userInput);

        if (existsRemainingQuestion())
        {
            loadNextQuestion();
        } 
        else
        {
            displayQuizSummary();
        }
    }

    /*
     * Evaluates the user's response and updates the score or missed collections.
     * @param userInput the answer provided by the user
     */
    private final void evaluateUserAnswer(final String userInput)
    {
        final StringBuilder scoreBuilder;
        scoreBuilder = new StringBuilder();

        if (userInput.equalsIgnoreCase(currentAnswer))
        {
            currentScore++;
            scoreBuilder.append("Score: ")
                        .append(currentScore);
            scoreLabel.setText(scoreBuilder.toString());
        } 
        else
        {
            missedQuestions.add(currentQuestion);
            missedAnswers.add(currentAnswer);
            incorrectQuestions.add(currentQuestionNumber);
        }
    }

    /*
     * Checks if the quiz session has more questions remaining.
     * @return true if below the total question count
     */
    private final boolean existsRemainingQuestion()
    {
        return currentQuestionNumber < TOTAL_QUESTIONS_COUNT;
    }

    /*
     * Advances to the next question and updates the interface.
     */
    private final void loadNextQuestion()
    {
        final StringBuilder nextQuestionBuilder;
        nextQuestionBuilder = new StringBuilder();

        currentQuestionNumber++;
        getNextQuestion();

        // Applied StringBuilder for iteration updates
        nextQuestionBuilder.append("Question ")
                           .append(currentQuestionNumber)
                           .append(" of ")
                           .append(TOTAL_QUESTIONS_COUNT)
                           .append(": ")
                           .append(currentQuestion);

        questionLabel.setText(nextQuestionBuilder.toString());
        answerTextField.clear();
    }

    /*
     * Compiles a final report of missed questions and correct answers.
     */
    private final void displayQuizSummary()
    {
        final StringBuilder summaryBuilder;
        summaryBuilder = new StringBuilder();
        
        summaryBuilder.append("Quiz completed! Final Score: ")
                      .append(currentScore)
                      .append("/")
                      .append(TOTAL_QUESTIONS_COUNT);

        if (!missedQuestions.isEmpty())
        {
            summaryBuilder.append("\n\n--- Correct Solutions ---");
            for (int i = 0; i < missedQuestions.size(); i++)
            {
                summaryBuilder.append("\n\nQ")
                              .append(incorrectQuestions.get(i))
                              .append(": ")
                              .append(missedQuestions.get(i))
                              .append("\nA: ")
                              .append(missedAnswers.get(i));
            }
        }

        summaryTextArea.setText(summaryBuilder.toString());
        summaryTextArea.setVisible(true);
        questionLabel.setText("Quiz Summary");
        answerTextField.setVisible(false);
        submitButton.setVisible(false);
        restartButton.setVisible(true);
    }

    /*
     * Resets score and UI state to begin a new attempt.
     */
    private final void restartQuiz()
    {
        final StringBuilder resetLabelBuilder;
        final StringBuilder scoreResetBuilder;
        
        resetLabelBuilder = new StringBuilder();
        scoreResetBuilder = new StringBuilder();

        currentScore          = BASE_SCORE;
        currentQuestionNumber = FIRST_QUESTION_NUMBER;

        scoreResetBuilder.append("Score: ").append(currentScore);
        scoreLabel.setText(scoreResetBuilder.toString());

        answerTextField.clear();
        answerTextField.setVisible(true);
        summaryTextArea.setVisible(false);
        summaryTextArea.clear();
        
        submitButton.setVisible(true);
        restartButton.setVisible(false);
        
        missedQuestions.clear();
        missedAnswers.clear();
        incorrectQuestions.clear();
        
        generateQuestionIndices();
        getNextQuestion();

        // Applied StringBuilder for reset text
        resetLabelBuilder.append("Question ")
                         .append(currentQuestionNumber)
                         .append(" of ")
                         .append(TOTAL_QUESTIONS_COUNT)
                         .append(" : ")
                         .append(currentQuestion);

        questionLabel.setText(resetLabelBuilder.toString());
    }

    /**
     * Entry point for the application.
     * @param args command line arguments (unused)
     */
    public static void main(final String[] args)
    {
        Application.launch(args);
    }
}
