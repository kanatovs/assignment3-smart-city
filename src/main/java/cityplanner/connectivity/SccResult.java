package cityplanner.connectivity;

import java.util.ArrayList;
import java.util.List;

public record SccResult(List<List<Integer>> components) {
    public SccResult {
        List<List<Integer>> snapshot = new ArrayList<>(components.size());
        for (List<Integer> component : components) {
            List<Integer> members = List.copyOf(component);
            if (members.isEmpty()) {
                throw new IllegalArgumentException("Strongly connected components must not be empty.");
            }
            snapshot.add(members);
        }
        components = List.copyOf(snapshot);
    }

    public int componentCount() {
        return components.size();
    }
}
