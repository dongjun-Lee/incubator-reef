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
package org.apache.reef.tests.evaluatorsize;

import org.apache.reef.driver.evaluator.AllocatedEvaluator;
import org.apache.reef.driver.evaluator.EvaluatorRequest;
import org.apache.reef.driver.evaluator.EvaluatorRequestor;
import org.apache.reef.driver.task.TaskConfiguration;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.tang.annotations.Unit;
import org.apache.reef.tang.exceptions.BindException;
import org.apache.reef.tests.library.exceptions.DriverSideFailure;
import org.apache.reef.wake.EventHandler;
import org.apache.reef.wake.time.event.StartTime;

import javax.inject.Inject;
import java.util.logging.Logger;

@Unit
final class EvaluatorSizeTestDriver {
  private static final Logger LOG = Logger.getLogger(EvaluatorSizeTestDriver.class.getName());

  private final EvaluatorRequestor evaluatorRequestor;

  private final int memorySize;

  @Inject
  public EvaluatorSizeTestDriver(final EvaluatorRequestor evaluatorRequestor,
                                 @Parameter(EvaluatorSizeTestConfiguration.MemorySize.class) final int memorySize) {
    this.evaluatorRequestor = evaluatorRequestor;
    this.memorySize = memorySize;
  }

  final class StartHandler implements EventHandler<StartTime> {
    @Override
    public void onNext(final StartTime startTime) {
      EvaluatorSizeTestDriver.this.evaluatorRequestor.submit(EvaluatorRequest.newBuilder()
          .setNumber(1)
          .setMemory(EvaluatorSizeTestDriver.this.memorySize)
          .setNumberOfCores(1)
          .build());
    }
  }

  final class EvaluatorAllocatedHandler implements EventHandler<AllocatedEvaluator> {

    @Override
    public void onNext(final AllocatedEvaluator allocatedEvaluator) {

      final int evaluatorMemory = allocatedEvaluator.getEvaluatorDescriptor().getMemory();

      if (evaluatorMemory < EvaluatorSizeTestDriver.this.memorySize) {
        throw new DriverSideFailure(
            "Got an Evaluator with too little RAM. Asked for " + EvaluatorSizeTestDriver.this.memorySize
                + "MB, but got " + evaluatorMemory + "MB.");
      }

      // ALL good on the Driver side. Let's move on to the Task
      try {
        final Configuration taskConfiguration = TaskConfiguration.CONF
            .set(TaskConfiguration.TASK, MemorySizeTask.class)
            .set(TaskConfiguration.IDENTIFIER, "EvaluatorSizeTestTask")
            .build();

        final Configuration testConfiguration = EvaluatorSizeTestConfiguration.CONF
            .set(EvaluatorSizeTestConfiguration.MEMORY_SIZE, EvaluatorSizeTestDriver.this.memorySize)
            .build();

        final Configuration mergedTaskConfiguration = Tang.Factory.getTang()
            .newConfigurationBuilder(taskConfiguration, testConfiguration).build();

        allocatedEvaluator.submitTask(mergedTaskConfiguration);

      } catch (final BindException e) {
        throw new DriverSideFailure("Unable to launch Task", e);
      }
    }
  }
}
