import javafx.beans.property.SimpleStringProperty
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException


import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream


import static groovyx.javafx.GroovyFX.start

import groovyx.javafx.beans.FXBindable
import javafx.scene.layout.GridPane

class Archive {
    @FXBindable
    String file, directory

    String toString() { "<$file>  : $directory" }
}

def selectedFile = new SimpleStringProperty("");
def selectedDir = new SimpleStringProperty("");

try {
    SevenZip.initSevenZipFromPlatformJAR();
    println("7-Zip-JBinding library was initialized");
} catch (SevenZipNativeInitializationException e) {
    e.printStackTrace();
}

start { app ->

    def archive = new Archive()

    final fileChooser = fileChooser(initialDirectory: ".", title: "FileChooser Demo") {
        filter("files", extensions: ["*.rar", "*.7z", "*.zip"])
    }

    final dirChooser = directoryChooser(initialDirectory: ".", title: "DirectoryChooserDemo")

    def logProperty = new SimpleStringProperty("")

    stage title: "解压", visible: true, {
        scene {
            gridPane hgap: 5, vgap: 10, padding: 25, alignment: TOP_CENTER,
                    style: "-fx-background-color: GROOVYBLUE", {
                columnConstraints minWidth: 50, halignment: "right"
                columnConstraints prefWidth: 250, hgrow: 'always'

                effect innerShadow()

                label "解压缩rar、7zip、zip", style: "-fx-font-size: 18px;", row: 0, textFill: WHITE,
                        columnSpan: GridPane.REMAINING, halignment: "center", margin: [0, 0, 10], {
                    onMouseEntered { e -> e.source.parent.gridLinesVisible = true }
                    onMouseExited { e -> e.source.parent.gridLinesVisible = false }
                }

                label "文件", hgrow: "never", row: 1, column: 0, textFill: WHITE
                textField id: 'fileTF', row: 1, column: 1
                button("文件", row: 1, column: 2, onAction: {
                    file = fileChooser.showOpenDialog(primaryStage)
                    selectedFile.set(file ? file.toString() : "")
                })

                label "目标目录", row: 2, column: 0, textFill: WHITE
                textField id: 'dirTF', row: 2, column: 1
                button("目录", row: 2, column: 2, onAction: {
                    dir = dirChooser.showDialog(primaryStage)
                    selectedDir.set(dir ? dir.toString() : "")
                })

                label "日志", row: 3, column: 0, valignment: "baseline", textFill: WHITE
                textArea id: 'log', prefRowCount: 8, row: 3, column: 1, vgrow: 'always'

                button "解压", row: 3, column: 2, halignment: "right", {
                    onAction {
                        SevenZip.openInArchive(null, new RandomAccessFileInStream(new RandomAccessFile(archive.file, "r"))).withCloseable { inArchive ->
                            inArchive.simpleInterface.archiveItems.each { item ->

                                try {
                                    def target = archive.directory + "/" + item.path

                                    if (!item.isFolder()) {
                                        ExtractOperationResult result
                                        def f = new File(target)
                                        f.getParentFile().mkdirs();
                                        new FileOutputStream(f).withCloseable { fileOutputStream ->
                                            def byteArrayOutputStream = new ByteArrayOutputStream()
                                            result = item.extractSlow { bytes ->
                                                byteArrayOutputStream.write(bytes)
                                                bytes.length // Return amount of consumed data
                                            }
                                            byteArrayOutputStream.writeTo(fileOutputStream)
                                        }

                                        if (result == ExtractOperationResult.OK) {
                                            logProperty.set("extract $target success");
                                        } else {
                                            logProperty.set("extract $target $result")
                                        }
                                    }
                                } catch (e) {
                                    log.set(e.message)
                                }

                            }
                        }

                    }
                }
            }
        }
    }
    archive.fileProperty().bind(selectedFile)
    archive.directoryProperty().bind(selectedDir)
    fileTF.textProperty().bind(selectedFile)
    dirTF.textProperty().bind(selectedDir)
    log.textProperty().bind(logProperty)
}