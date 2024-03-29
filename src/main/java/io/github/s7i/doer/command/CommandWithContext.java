package io.github.s7i.doer.command;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.config.Base;

import static java.util.Objects.requireNonNull;

public abstract class CommandWithContext<M extends Base> extends ManifestFileCommand {

    protected abstract Class<M> manifestClass();

    @Override
    public void onExecuteCommand() {
        var manifest = requireNonNull(parseYaml(manifestClass()), "manifest definition");

        var ctx = new Context.Initializer(Context.InitialParameters.builder()
                .workDir(yaml.toPath().toAbsolutePath().getParent())
                .params(manifest.getParams())
                .build())
                .context();
        onExecuteCommand(ctx, manifest);
    }

    public abstract void onExecuteCommand(Context context, M manifest);
}
