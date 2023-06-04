/*
 * MIT License
 *
 * Copyright (c) 2014 Aneesh Joseph
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.coderplus.plugins;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Copy files during build
 *
 * @author <a href="aneesh@coderplus.com">Aneesh Joseph</a>
 * @since 1.0
 */
@Mojo(name = "copy", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CopyMojo extends AbstractMojo {
    /**
     * The file which has to be copied
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private File sourceFile;

    /**
     * The target file to which the file should be copied(this shouldn't be a
     * directory but a file
     * which does or does not exist)
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private File destinationFile;

    /**
     * Collection of FileSets to work on (FileSet contains sourceFile and
     * destinationFile). See <a
     * href="./usage.html">Usage</a> for details.
     *
     * @since 1.0
     */
    @Parameter(required = false)
    private List<FileSet> fileSets;

    /**
     * Overwrite files
     *
     * @since 1.0
     */
    @Parameter(property = "copy.overWrite", defaultValue = "true")
    boolean overWrite;

    /**
     * Ignore File Not Found errors during incremental build
     *
     * @since 1.0
     */
    @Parameter(property = "copy.ignoreFileNotFoundOnIncremental", defaultValue = "true")
    boolean ignoreFileNotFoundOnIncremental;

    /**
     * @since 1.0
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().debug("Executing the copy-rename-maven-plugin");
        if (fileSets != null && fileSets.size() > 0) {
            for (FileSet fileSet : fileSets) {
                File srcFile = fileSet.getSourceFile();
                File destFile = fileSet.getDestinationFile();
                if (srcFile != null) {
                    copy(srcFile, destFile);
                }
            }
        } else if (sourceFile != null) {
            copy(sourceFile, destinationFile);
        } else {
            getLog().info("No Files to process");
        }
    }

    private void copy(File srcFile, File destFile) throws MojoExecutionException {

        if (!srcFile.exists()) {
            if (ignoreFileNotFoundOnIncremental && buildContext.isIncremental()) {
                getLog().warn("sourceFile " + srcFile.getAbsolutePath() + " not found during incremental build");
            } else {
                getLog().error("sourceFile " + srcFile.getAbsolutePath() + " does not exist");
            }
        } else if (srcFile.isDirectory()) {
            getLog().error("sourceFile " + srcFile.getAbsolutePath() + " is not a file");
        } else if (destFile == null) {
            getLog().error("destinationFile not specified");
        } else if (destFile.exists() && destFile.isFile() && !overWrite) {
            getLog().error(destFile.getAbsolutePath() + " already exists and overWrite not set");
        } else {
            try {
                if (buildContext.isIncremental()
                        && destFile.exists()
                        && !buildContext.hasDelta(srcFile)
                        && FileUtils.contentEquals(srcFile, destFile)) {
                    getLog().info("No changes detected in " + srcFile.getAbsolutePath());
                    return;
                }
                FileUtils.copyFile(srcFile, destFile);
                getLog().info("Copied " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                buildContext.refresh(destFile);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "could not copy " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
            }
        }
    }
}
