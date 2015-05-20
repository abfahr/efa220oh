/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.statistics;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;

public class StatisticCSVWriter extends StatisticWriter {

  private String encoding;
  private String separator;
  private String quotes;
  private int linelength = 0;

  public StatisticCSVWriter(StatisticsRecord sr, StatisticsData[] sd) {
    super(sr, sd);
    this.encoding = sr.sOutputEncoding;
    this.separator = sr.sOutputCsvSeparator;
    this.quotes = sr.sOutputCsvQuotes;
    if (this.encoding == null || this.encoding.length() == 0) {
      this.encoding = Daten.ENCODING_UTF;
    }
    if (this.separator == null || this.separator.length() == 0) {
      this.separator = "|";
    }
    if (this.quotes != null && this.quotes.length() == 0) {
      this.quotes = null;
    }
  }

  protected synchronized void write(BufferedWriter fw, String s) throws IOException {
    if (s == null) {
      s = "";
    }
    if (quotes == null && s.indexOf(separator) >= 0) {
      String repl = (!separator.equals("_") ? "_" : "#");
      s = EfaUtil.replace(s, separator, repl, true);
    }
    fw.write((linelength > 0 ? separator : "") +
        (quotes != null ? quotes : "") + s + (quotes != null ? quotes : ""));
    linelength += s.length();
  }

  protected synchronized void writeln(BufferedWriter fw) throws IOException {
    fw.write("\n");
    linelength = 0;
  }

  @Override
  public boolean write() {
    BufferedWriter f = null;

    if (sr.sFileExecBefore != null && sr.sFileExecBefore.length() > 0) {
      EfaUtil.execCmd(sr.sFileExecBefore);
    }
    try {
      // Create File
      f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sr.sOutputFile), encoding));

      // Write normal Output
      if (sr.pTableColumns != null && sr.pTableColumns.size() > 0) {
        for (int i = 0; i < sr.pTableColumns.size(); i++) {
          write(f, sr.pTableColumns.get(i));
        }
        writeln(f);

        for (StatisticsData element : sd) {
          if (element.isMaximum || element.isSummary) {
            continue;
          }
          if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.list ||
              sr.sStatisticCategory == StatisticsRecord.StatisticCategory.matrix ||
              sr.sStatisticCategory == StatisticsRecord.StatisticCategory.other) {
            if (sr.sIsFieldsPosition) {
              write(f, element.sPosition);
            }
            if (sr.sIsFieldsName) {
              write(f, element.sName);
            }
            if (sr.sIsFieldsGender) {
              write(f, element.sGender);
            }
            if (sr.sIsFieldsStatus) {
              write(f, element.sStatus);
            }
            if (sr.sIsFieldsYearOfBirth) {
              write(f, element.sYearOfBirth);
            }
            if (sr.sIsFieldsBoatType) {
              write(f, element.sBoatType);
            }
            if (sr.sIsAggrDistance) {
              write(f, element.sDistance);
            }
            if (sr.sIsAggrRowDistance) {
              write(f, element.sRowDistance);
            }
            if (sr.sIsAggrCoxDistance) {
              write(f, element.sCoxDistance);
            }
            if (sr.sIsAggrSessions) {
              write(f, element.sSessions);
            }
            if (sr.sIsAggrAvgDistance) {
              write(f, element.sAvgDistance);
            }
            if (sr.sIsAggrDuration) {
              write(f, element.sDuration);
            }
            if (sr.sIsAggrSpeed) {
              write(f, element.sSpeed);
            }
            if (sr.sIsAggrZielfahrten) {
              write(f, element.sDestinationAreas);
            }
            if (sr.sIsAggrWanderfahrten) {
              write(f, element.sWanderfahrten);
            }
            if (sr.sIsAggrDamageCount) {
              write(f, element.sDamageCount);
            }
            if (sr.sIsAggrDamageDuration) {
              write(f, element.sDamageDuration);
            }
            if (sr.sIsAggrDamageAvgDuration) {
              write(f, element.sDamageAvgDuration);
            }
            if (sr.sIsAggrClubwork) {
              write(f, element.sClubwork);
            }
            if (sr.sIsAggrClubworkTarget) {
              write(f, element.sClubworkTarget);
            }
            if (sr.sIsAggrClubworkRelativeToTarget) {
              write(f, element.sClubworkRelativeToTarget);
            }
            if (sr.sIsAggrClubworkOverUnderCarryOver) {
              write(f, element.sClubworkOverUnderCarryOver);
            }
            if (sr.sIsAggrClubworkCredit) {
              write(f, element.sClubworkCredit);
            }
            if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.matrix) {
              for (int j = sr.pMatrixColumnFirst; j < sr.pTableColumns.size(); j++) {
                StatisticsData sdm = (element.matrixData != null ?
                    element.matrixData.get(sr.pMatrixColumns.get(sr.pTableColumns.get(j))) : null);
                write(f, getMatrixString(sdm));
              }
            }
          }
          if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.logbook) {
            if (element.logbookFields != null) {
              for (String logbookField : element.logbookFields) {
                write(f, logbookField);
              }
            }
          }
          if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.other) {
            if (element.otherFields != null) {
              for (String otherField : element.otherFields) {
                write(f, otherField);
              }
            }
          }
          writeln(f);
        }
      }
      f.close();
    } catch (IOException e) {
      Dialog.error(LogString.fileCreationFailed(sr.sOutputFile,
          International.getString("Ausgabedatei")));
      LogString
      .logError_fileCreationFailed(sr.sOutputFile, International.getString("Ausgabedatei"));
      resultMessage = LogString.fileCreationFailed(sr.sOutputFile,
          International.getString("Statistik"));
      return false;
    } finally {
      try {
        f.close();
      } catch (Exception ee) {
        f = null;
      }
    }
    if (sr.sFileExecAfter != null && sr.sFileExecAfter.length() > 0) {
      EfaUtil.execCmd(sr.sFileExecAfter);
    }
    resultMessage = LogString.fileSuccessfullyCreated(sr.sOutputFile,
        International.getString("Statistik"));
    return true;
  }
}
