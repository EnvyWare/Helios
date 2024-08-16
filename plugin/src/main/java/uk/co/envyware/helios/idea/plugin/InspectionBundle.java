package uk.co.envyware.helios.idea.plugin;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class InspectionBundle {

    public static final String BUNDLE = "messages.InspectionBundle";

    private static final DynamicBundle INSTANCE = new DynamicBundle(InspectionBundle.class, BUNDLE);

    private InspectionBundle() {
    }

    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

}
