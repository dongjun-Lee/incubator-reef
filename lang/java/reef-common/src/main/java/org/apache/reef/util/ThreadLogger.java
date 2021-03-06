/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.reef.util;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Methods to log the currently active set of threads with their stack traces. This is useful to log abnormal
 * process exit situations, for instance the Driver timeout in the tests.
 */
public final class ThreadLogger {

  /**
   * This is a utility class that shall not be instantiated.
   */
  private ThreadLogger() {
  }

  /**
   * Same as <code>logThreads(logger, level, prefix, "\n\t", "\n\t\t")</code>.
   */
  public static void logThreads(final Logger logger, final Level level, final String prefix) {
    logThreads(logger, level, prefix, "\n\t", "\n\t\t");
  }

  /**
   * Logs the currently active threads and their stack trace to the given Logger and Level.
   *
   * @param logger             the Logger instance to log to.
   * @param level              the Level to log into.
   * @param prefix             a prefix of the log message.
   * @param threadPrefix       logged before each thread, e.g. "\n\t" to create an indented list.
   * @param stackElementPrefix logged before each stack trace element, e.g. "\n\t\t" to create an indented list.
   */
  public static void logThreads(
      final Logger logger, final Level level, final String prefix,
      final String threadPrefix, final String stackElementPrefix) {
    logger.log(level, getFormattedThreadList(prefix, threadPrefix, stackElementPrefix));
  }

  /**
   * Produces a String representation of the currently running threads.
   *
   * @param prefix             The prefix of the string returned.
   * @param threadPrefix       Printed before each thread, e.g. "\n\t" to create an indented list.
   * @param stackElementPrefix Printed before each stack trace element, e.g. "\n\t\t" to create an indented list.
   * @return a String representation of the currently running threads.
   */
  public static String getFormattedThreadList(
      final String prefix, final String threadPrefix, final String stackElementPrefix) {
    final StringBuilder message = new StringBuilder(prefix);
    for (final Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      message.append(threadPrefix).append("Thread '").append(entry.getKey().getName()).append("':");
      for (final StackTraceElement element : entry.getValue()) {
        message.append(stackElementPrefix).append(element.toString());
      }
    }
    return message.toString();
  }

  /**
   * Same as <code>getFormattedThreadList(prefix, "\n\t", "\n\t\t")</code>.
   */
  public static String getFormattedThreadList(final String prefix) {
    return getFormattedThreadList(prefix, "\n\t", "\n\t\t");
  }

  /**
   * Produces a String representation of threads that are deadlocked, including lock information.
   * @param prefix             The prefix of the string returned.
   * @param threadPrefix       Printed before each thread, e.g. "\n\t" to create an indented list.
   * @param stackElementPrefix Printed before each stack trace element, e.g. "\n\t\t" to create an indented list.
   * @return a String representation of threads that are deadlocked, including lock information
   */
  public static String getFormattedDeadlockInfo(
      final String prefix, final String threadPrefix, final String stackElementPrefix) {
    final StringBuilder message = new StringBuilder(prefix);

    final DeadlockInfo deadlockInfo = new DeadlockInfo();
    for (final ThreadInfo threadInfo : deadlockInfo.getDeadlockedThreads()) {
      message.append(threadPrefix).append("Thread '").append(threadInfo.getThreadName())
          .append("' with state ").append(threadInfo.getThreadState());

      boolean firstElement = true;
      for (final StackTraceElement stackTraceElement : threadInfo.getStackTrace()) {
        message.append(stackElementPrefix).append("at ").append(stackTraceElement);
        if (firstElement) {
          final String waitingLockString = deadlockInfo.getWaitingLockString(threadInfo);
          if (waitingLockString != null) {
            message.append(stackElementPrefix).append("- waiting to lock: ").append(waitingLockString);
          }
          firstElement = false;
        }
        for (final MonitorInfo info : deadlockInfo.getMonitorLockedElements(threadInfo, stackTraceElement)) {
          message.append(stackElementPrefix).append("- locked: ").append(info);
        }
      }
      for (final LockInfo lockInfo : threadInfo.getLockedSynchronizers()) {
        message.append(stackElementPrefix).append("* holds locked synchronizer: ").append(lockInfo);
      }
    }

    return message.toString();
  }

  /**
   * Same as <code>getFormattedDeadlockInfo(prefix, "\n\t", "\n\t\t")</code>.
   */
  public static String getFormattedDeadlockInfo(final String prefix) {
    return getFormattedDeadlockInfo(prefix, "\n\t", "\n\t\t");
  }

  /**
   * An example how to use the above methods.
   *
   * @param args ignored.
   */
  public static void main(final String[] args) {
    logThreads(Logger.getAnonymousLogger(), Level.INFO, "Threads active:");
  }
}
