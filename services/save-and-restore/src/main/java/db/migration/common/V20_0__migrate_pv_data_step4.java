/*
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package db.migration.common;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Migrates readback values such that both the value and readback value - where applicable - is
 * contained in a single record in the snapshot_node_pv table.
 * @author georgweiss
 * Created 24 Jan 2020
 */
public class V20_0__migrate_pv_data_step4 extends BaseJavaMigration{

    private static final int NO_ID = -1;

    @Override
    public void migrate(Context context) throws Exception {

        try (Statement statement = context.getConnection().createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT id FROM node WHERE type='SNAPSHOT'")) {
                while (rows.next()) {
                    int snapshotNodeId = rows.getInt("id");
                    processSnapshot(context, snapshotNodeId);
                }
            }
        }
    }

    private void processSnapshot(Context context, int snapshotNodeId) throws Exception{
        try (Statement statement = context.getConnection().createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT config_pv_id FROM snapshot_node_pv " +
                    "WHERE snapshot_node_id=" + snapshotNodeId + " AND readback=false")) {
                while (rows.next()){
                    Integer configPvId = rows.getInt("config_pv_id");
                    try(Statement statement2 = context.getConnection().createStatement()){
                        try(ResultSet rows2 = statement2.executeQuery("SELECT * FROM snapshot_node_pv WHERE config_pv_id=" +
                                configPvId + " AND snapshot_node_id= " + snapshotNodeId + " AND readback=true")){
                            // Zero or one result
                            while(rows2.next()){
                                long readbackTime = rows2.getLong("time");
                                int readbackTimens = rows2.getInt("timens");
                                String readbackValue = rows2.getString("value");
                                String readbackSizes = rows2.getString("sizes");
                                String readbackStatus = rows2.getString("status");
                                String readbackSeverity = rows2.getString("severity");
                                String reacbackDataType = rows2.getString("data_type");
                                String sql = "UPDATE snapshot_node_pv SET readback_time=?," +
                                        "readback_timens=?, " +
                                        "readback_value=?, " +
                                        "readback_sizes=?, " +
                                        "readback_status=?, " +
                                        "readback_severity=?, " +
                                        "readback_data_type=? " +
                                        "WHERE snapshot_node_id=? AND config_pv_id=? AND readback=false";
                                try(PreparedStatement statement3 = context.getConnection().prepareStatement(sql)){
                                    statement3.setLong(1, readbackTime);
                                    statement3.setInt(2, readbackTimens);
                                    statement3.setString(3, readbackValue);
                                    statement3.setString(4, readbackSizes);
                                    statement3.setString(5, readbackStatus);
                                    statement3.setString(6, readbackSeverity);
                                    statement3.setString(7, reacbackDataType);
                                    statement3.setInt(8, snapshotNodeId);
                                    statement3.setInt(9, configPvId);
                                    statement3.execute();
                                    context.getConnection().commit();
                                    try(Statement statement4 = context.getConnection().createStatement()){
                                        statement4.execute("DELETE FROM snapshot_node_pv WHERE snapshot_node_id=" + snapshotNodeId + " AND config_pv_id=" + configPvId + " AND readback=true");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
