/*
 * Author: atotic
 * Created: Aug 16, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;


public class RegularLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate{
    /**
     * @return
     */
    protected String getRunnerConfigRun() {
        return PythonRunnerConfig.RUN_REGULAR;
    }

}
