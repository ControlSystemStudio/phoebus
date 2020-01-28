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

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Adds pv_id values to snapshot_node_pv table.
 * @author georgweiss
 * Created 24 Jan 2020
 */
public class V20_0__migrate_pv_data_step4 extends BaseJavaMigration{

    private static final int NO_ID = -1;

    @Override
    public void migrate(Context context) throws Exception {

        try (Statement statement = context.getConnection().createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT * FROM snapshot_node_pv")) {
                while (rows.next()) {
                    int snapshotNodeId = rows.getInt("snapshot_node_id");
                    int configPvId = rows.getInt("config_pv_id");
                    boolean isReadback = rows.getBoolean("readback");
                    int pvId = getPvId(context, configPvId, isReadback);
                    try (Statement statement2 = context.getConnection().createStatement()){
                        statement2.execute("UPDATE snapshot_node_pv SET pv_id=" + pvId + " WHERE snapshot_node_id=" + snapshotNodeId +
                                " AND config_pv_id=" + configPvId);
                    }
                }
            }
        }
    }

    private int getPvId(Context context, int configPvId, boolean isReadback) throws Exception{
        Statement statement = context.getConnection().createStatement();
        int pvId = NO_ID;
        if(!isReadback){
            ResultSet rows = statement.executeQuery("SELECT pv_id FROM config_pv WHERE id=" + configPvId);
            // Only one result expected
            rows.next();
            pvId = rows.getInt("pv_id");
        }
        else{
            ResultSet rows = statement.executeQuery("SELECT readback_pv_id FROM config_pv WHERE id=" + configPvId);
            // Only one result expected
            rows.next();
            pvId = rows.getInt("readback_pv_id");
        }
        statement.close();
        return pvId;
    }
}
