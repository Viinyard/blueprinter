package pro.vinyard.vb.shell.core.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import pro.vinyard.vb.engine.core.environment.EnvironmentManager;
import pro.vinyard.vb.engine.core.exception.VelocityBlueprintException;
import pro.vinyard.vb.engine.core.model.ModelManager;
import pro.vinyard.vb.shell.shell.CustomAbstractShellComponent;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shell component to manage models
 * <p>It allows to create, delete, list and check models</p>
 */
@ShellComponent
@Slf4j
public class ModelShellComponent extends CustomAbstractShellComponent {

    private final ModelManager modelManager;

    private final EnvironmentManager environmentManager;

    public ModelShellComponent(ModelManager modelManager, EnvironmentManager environmentManager) {
        this.modelManager = modelManager;
        this.environmentManager = environmentManager;
    }

    /**
     * Create a model
     * <p>Ask the user for a model name and create the model</p>
     * <p>The model is generated by :</p>
     * <ul>
     *     <li>Creating a folder with the model name in the model directory</li>
     *     <li>Copying the model configuration file {@code model.xml} in the model folder</li>
     *     <li>Copying the model schema file {@code model.xsd} in the model folder</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the model name is null
     */
    public void createModel() {
        String name = stringInput("Enter model name", null, false);

        if (name == null) {
            throw new IllegalArgumentException("Model name cannot be null");
        }

        this.modelManager.createModel(name);
    }

    /**
     * Delete models
     * <p>Ask the user to select models to delete and delete them.</p>
     * <p>Models are deleted by deleting all selected models folders.</p>
     *
     * @throws IllegalArgumentException if no model is selected
     */
    public void deleteModels() {
        List<SelectorItem<String>> items = this.modelManager.findAllModels().stream().map(m -> SelectorItem.of(m, m)).collect(Collectors.toList());

        List<String> models = multiSelector(items, "Select models to delete");

        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("No model selected.");
        }

        for (String model : models) {
            this.modelManager.deleteModel(model);
        }
    }

    /**
     * Check models
     * <p>Ask the user to select models to check and check them.</p>
     * <p>Models are checked by :</p>
     * <ul>
     *     <li>Checking if the model directory exist.</li>
     *     <li>Checking if the model configuration file {@code model.xml} exist.</li>
     *     <li>Checking if the model schema file {@code model.xsd} exist.</li>
     *     <li>Checking if all templates in the model configuration file exists.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if no model is selected
     */
    public void checkModels() {
        List<SelectorItem<String>> items = this.modelManager.findAllModels().stream().map(m -> SelectorItem.of(m, m)).collect(Collectors.toList());

        List<String> models = multiSelector(items, "Select models to check");

        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("No model selected.");
        }

        for (String model : models) {
            try {
                this.modelManager.checkModel(model);
            } catch (VelocityBlueprintException e) {
                log.error("Model {} is invalid : {}", model, e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * List models
     * <p>List all models</p>
     * <p>Models are listed by listing all models names. Each models have his own directory in the model directory.</p>
     */
    public void listModels() {
        String msg = this.modelManager.listModels();

        PrintWriter printWriter = this.getTerminal().writer();
        printWriter.print(msg);
        printWriter.flush();
    }


    /**
     * Ask the user to select an action on model
     *
     * <p>Actions are :</p>
     * <ul>
     *     <li>Create model</li>
     *     <li>Delete model</li>
     *     <li>List models</li>
     *     <li>Check models</li>
     * </ul>
     */
    @ShellMethod(key = "model", value = "Action on model", group = "Model")
    @ShellMethodAvailability("environmentAvailability")
    public void model() {
        List<SelectorItem<Runnable>> items = Arrays.asList(
                SelectorItem.of("Create model", this::createModel),
                SelectorItem.of("Delete model", this::deleteModels),
                SelectorItem.of("List models", this::listModels),
                SelectorItem.of("Check models", this::checkModels)
        );

        singleSelect(items, "Select action").run();
    }

    /**
     * Check if the environment is initialized to get the availability of the {@code model} command
     *
     * @return {@link Availability#available()} if the environment is initialized, {@link Availability#unavailable(String)} otherwise
     */
    public Availability environmentAvailability() {
        return environmentManager.checkEnvironmentInitialized() ? Availability.available() : Availability.unavailable("Environment not initialized.");
    }
}
