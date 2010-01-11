/*
 * Copyright 2010 @ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ashigeru.lab.smalltable.client.SmallTable;
import com.ashigeru.lab.smalltable.client.StObject;
import com.ashigeru.lab.smalltable.local.LocalRepository;

/**
 * 確認用のサンプル。
 * @author ashigeru
 */
public class StMain {

    private static final Logger LOG = LoggerFactory.getLogger(StMain.class);

    private SmallTable table;

    /**
     * インスタンスを生成する。
     * @param table 利用するテーブル
     */
    public StMain(SmallTable table) {
        assert table != null;
        this.table = table;
    }

    private void start() {
        StObject root = table.getRootObject("hello");
        if (root == null) {
            root = table.newObject();
            table.setRootObject("hello", root);
        }

        StObject child = table.newObject();
        root.setProperty("world", child);

        child.setProperty("value", "world!");
        table.save();
    }

    /**
     * プログラムエントリ
     * @param args {@code [-i <読み出すリポジトリ>] [-o <書き出すリポジトリ>]}
     * @throws IOException リポジトリの読み書きに失敗した場合
     */
    public static void main(String[] args) throws IOException {
        Iterator<String> iter = Arrays.asList(args).iterator();
        File input = null;
        File output = null;
        while (iter.hasNext()) {
            String string = iter.next();
            if (string.equals("-i")) {
                input = new File(iter.next());
            }
            else if (string.equals("-o")) {
                output = new File(iter.next());
            }
            else {
                throw new IllegalArgumentException("Unrecognized option: " + string);
            }
        }

        LocalRepository repo;
        if (input != null) {
            repo = load(input);
        }
        else {
            repo = new LocalRepository();
        }
        SmallTable table = new SmallTable(repo.createSession());
        StMain main = new StMain(table);
        main.start();

        if (output != null) {
            store(repo, output);
        }
    }

    private static LocalRepository load(File file) throws IOException {
        assert file != null;
        LOG.info("Loading Repository from {}", file);
        InputStream input = new FileInputStream(file);
        try {
            ObjectInputStream in = new ObjectInputStream(input);
            return (LocalRepository) in.readObject();
        }
        catch (ClassNotFoundException e) {
            throw (IOException) new IOException().initCause(e);
        }
        finally {
            try {
                input.close();
            }
            catch (IOException e) {
                return null;
            }
        }
    }

    private static void store(LocalRepository repo, File file) throws IOException {
        assert repo != null;
        assert file != null;
        LOG.info("Storing Repository to {}", file);
        OutputStream output;
        try {
            output = new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }
        try {
            ObjectOutputStream out = new ObjectOutputStream(output);
            out.writeObject(repo);
            out.close();
        }
        finally {
            try {
                output.close();
            }
            catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}
