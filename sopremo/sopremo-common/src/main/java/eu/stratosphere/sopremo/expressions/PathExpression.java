package eu.stratosphere.sopremo.expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import eu.stratosphere.sopremo.EvaluationContext;

@OptimizerHints(scope = { Scope.OBJECT, Scope.ARRAY })
public class PathExpression extends ContainerExpression {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4663949354781572815L;

	private final List<SopremoExpression<EvaluationContext>> fragments = new ArrayList<SopremoExpression<EvaluationContext>>();

	public PathExpression(final EvaluationExpression... fragments) {
		this(Arrays.asList(fragments));
	}

	public PathExpression(final List<? extends EvaluationExpression> fragments) {
		for (final EvaluationExpression evaluableExpression : fragments)
			if (evaluableExpression instanceof PathExpression)
				this.fragments.addAll(((PathExpression) evaluableExpression).fragments);
			else
				this.fragments.add(evaluableExpression);
	}

	public void add(final EvaluationExpression fragment) {
		this.fragments.add(fragment);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PathExpression other = (PathExpression) obj;
		return this.fragments.equals(other.fragments);
	}

	@Override
	public JsonNode evaluate(final JsonNode node, final EvaluationContext context) {
		JsonNode fragmentNode = node;
		for (final SopremoExpression<EvaluationContext> fragment : this.fragments)
			fragmentNode = fragment.evaluate(fragmentNode, context);
		return fragmentNode;
	}

	public int getDepth() {
		return this.fragments.size();
	}

	public SopremoExpression<EvaluationContext> getFragment(final int index) {
		return this.fragments.get(index);
	}

	public List<SopremoExpression<EvaluationContext>> getFragments() {
		return this.fragments;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.fragments.hashCode();
		return result;
	}

	public boolean isPrefix(final PathExpression prefix) {
		if (this.fragments.size() < prefix.getDepth())
			return false;
		return this.fragments.subList(0, prefix.getDepth()).equals(prefix.fragments);
	}

	@Override
	public Iterator<SopremoExpression<EvaluationContext>> iterator() {
		return this.fragments.iterator();
	}

	@Override
	public void replace(final EvaluationExpression toReplace, final EvaluationExpression replaceFragment) {
		super.replace(toReplace, replaceFragment);

		final PathExpression pathToFind = this.wrapAsPath(toReplace);
		final PathExpression replacePath = this.wrapAsPath(replaceFragment);
		int size = this.fragments.size() - pathToFind.fragments.size() + 1;
		final int findSize = pathToFind.fragments.size();
		findStartIndex: for (int startIndex = 0; startIndex < size; startIndex++) {
			for (int index = 0; index < findSize; index++)
				if (!this.fragments.get(startIndex + index).equals(pathToFind.fragments.get(index)))
					continue findStartIndex;

			this.fragments.subList(startIndex, startIndex + findSize).clear();
			this.fragments.addAll(startIndex, replacePath.fragments);
			size -= findSize - replacePath.fragments.size();
			// startIndex += replacePath.fragments.size();
		}
	}

	@Override
	protected void toString(final StringBuilder builder) {
		for (final SopremoExpression<EvaluationContext> fragment : this.fragments)
			fragment.toString(builder);
	}

	private PathExpression wrapAsPath(final EvaluationExpression expression) {
		if (expression instanceof PathExpression)
			return (PathExpression) expression;
		return new PathExpression(expression);
	}

	public static class Writable extends PathExpression implements WritableEvaluable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2014987314121118540L;

		public <T extends EvaluationExpression & WritableEvaluable> Writable(final List<T> fragments) {
			super(fragments);
		}

		public <T extends EvaluationExpression & WritableEvaluable> Writable(final T... fragments) {
			super(fragments);
		}

		@Override
		public void add(final EvaluationExpression fragment) {
			if (!(fragment instanceof WritableEvaluable))
				throw new IllegalArgumentException();
			super.add(fragment);
		}

		@Override
		public void set(final JsonNode node, final JsonNode value, final EvaluationContext context) {
			JsonNode fragmentNode = node;
			final List<SopremoExpression<EvaluationContext>> fragments = this.getFragments();
			for (int index = 0; index < fragments.size() - 1; index++)
				fragmentNode = fragments.get(index).evaluate(fragmentNode, context);
			((WritableEvaluable) fragmentNode).set(node, value, context);
		}
	}
}