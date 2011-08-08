package eu.stratosphere.sopremo.cleansing.fusion;

import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.NullNode;

import eu.stratosphere.sopremo.pact.JsonNodeComparator;

public class MergeDistinctRule extends FusionRule {
	/**
	 * 
	 */
	private static final long serialVersionUID = -281898889096008741L;

	/**
	 * The default, stateless instance.
	 */
	public final static MergeDistinctRule INSTANCE = new MergeDistinctRule();

	@Override
	public JsonNode fuse(final JsonNode[] values, final double[] weights, final FusionContext context) {
		final ArrayNode array = new ArrayNode(null);
		final Set<JsonNode> distinctValues = new TreeSet<JsonNode>(JsonNodeComparator.INSTANCE);
		for (final JsonNode value : values)
			if (value != NullNode.getInstance())
				distinctValues.add(value);
		array.addAll(distinctValues);
		return array;
	}
}
