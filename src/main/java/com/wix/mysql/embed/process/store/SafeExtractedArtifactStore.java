package com.wix.mysql.embed.process.store;

import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.extract.ExtractedFileSets;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.file.FileCleaner;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import de.flapdoodle.embed.process.store.IDownloader;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * This is a wrapper around `ExtractedArtifactStore` which deletes the temp directory BEFORE extracting
 * just in case we have left overs from last crashed run.
 *
 * @author maximn
 * @since 22-Oct-2015
 */
public class SafeExtractedArtifactStore extends ExtractedArtifactStore {
    private static Logger logger = LoggerFactory.getLogger(SafeExtractedArtifactStore.class);
    private String directory;

    public SafeExtractedArtifactStore(IDownloadConfig downloadConfig, IDownloader downloader, DirectoryAndExecutableNaming extraction, DirectoryAndExecutableNaming directory) {
        super(downloadConfig, downloader, extraction, directory);
        this.directory = directory.getDirectory().asFile().getAbsolutePath();
    }

    @Override
    public IExtractedFileSet extractFileSet(Distribution distribution) throws IOException {
        FileUtils.deleteDirectory(new File(directory));

        return super.extractFileSet(distribution);
    }

    @Override
    public void removeFileSet(Distribution distribution, IExtractedFileSet all) {
        Iterator exe = EnumSet.complementOf(EnumSet.of(FileType.Executable)).iterator();

        while(exe.hasNext()) {
            FileType type = (FileType)exe.next();
            Iterator i$ = all.files(type).iterator();

            while(i$.hasNext()) {
                File file = (File)i$.next();
                if(file.exists() && !de.flapdoodle.embed.process.io.file.Files.forceDelete(file)) {
                    logger.warn("Could not delete {} NOW: {}", type, file);
                }
            }
        }

        File exe1 = all.executable();
        if(exe1.exists() && !forceDelete(exe1)) {
            logger.warn("Could not delete executable NOW: {}", exe1);
        }

        if(all.baseDirIsGenerated() && !forceDelete(all.baseDir())) {
            logger.warn("Could not delete generatedBaseDir: {}", all.baseDir());
        }

    }

    private static boolean forceDelete(File fileOrDir) {
        boolean ret = false;

        try {
            if(fileOrDir != null && fileOrDir.exists()) {
                FileUtils.forceDelete(fileOrDir);
                logger.debug("could delete {}", fileOrDir);
                ret = true;
            }
        } catch (IOException var3) {
            logger.error(String.format("could not delete %s. Will try to delete it again when program exits.", fileOrDir), var3);
            FileCleaner.forceDeleteOnExit(fileOrDir);
            ret = true;
        }

        return ret;
    }

}
