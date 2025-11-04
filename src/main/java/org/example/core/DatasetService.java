package org.example.core;

import org.example.model.FileRecord;

import java.io.File;
import java.util.List;

public interface DatasetService {
    List<FileRecord> list();
    FileRecord importFile(File file);
    void remove(int id);
}
