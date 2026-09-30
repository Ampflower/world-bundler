package gay.ampflower.bundler.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class SqlUtils {
	public static void transaction(
		final Connection connection,
		final Transaction action
	) throws SQLException {
		connection.setAutoCommit(false);
		final Savepoint savepoint = connection.setSavepoint();

		Throwable thrown = null;

		try {
			action.call(connection);
			connection.commit();
		} catch (Throwable t) {
			thrown = t;
			try {
				connection.rollback(savepoint);
			} catch (Throwable inner) {
				t.addSuppressed(inner);
			}
			throw t;
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (Throwable inner) {
				if (thrown != null) {
					thrown.addSuppressed(inner);
				} else {
					thrown = inner;
				}
			}
		}
		if (thrown == null) {
			return;
		}
		if (thrown instanceof SQLException sql) {
			throw sql;
		}
		throw terminate(thrown);
	}

	private static Error terminate(Throwable thrown) {
		if (thrown instanceof Error error) {
			throw error;
		}
		if (thrown instanceof RuntimeException runtime) {
			throw runtime;
		}
		return new AssertionError(thrown);
	}

	@FunctionalInterface
	private interface Durian {
		void call() throws Throwable;
	}

	@FunctionalInterface
	public interface Transaction {
		void call(Connection connection) throws SQLException;
	}
}
