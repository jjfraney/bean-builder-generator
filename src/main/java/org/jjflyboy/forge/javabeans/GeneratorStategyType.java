package org.jjflyboy.forge.javabeans;

/**
 * @author jfraney
 */
public enum GeneratorStategyType {
	BUILDER_ONLY, BUILDER_AND_UPDATER, LOADER_ONLY;

	public String toString() {
		return name().replace('_', '-');
	}
}
