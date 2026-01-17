import Foundation
import SQLite3

class LocationDatabase {
    static let shared = LocationDatabase()
    private var db: OpaquePointer?
    private let dbName = "background_tracker.sqlite"
    
    private init() {
        openDatabase()
        createTable()
    }
    
    private func openDatabase() {
        let fileURL = try! FileManager.default
            .url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent(dbName)
            
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening database")
        }
    }
    
    private func createTable() {
        let createTableString = """
        CREATE TABLE IF NOT EXISTS locations(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        latitude REAL,
        longitude REAL,
        accuracy REAL,
        altitude REAL,
        speed REAL,
        bearing REAL,
        timestamp REAL,
        activity TEXT,
        battery_level INTEGER,
        is_synced INTEGER DEFAULT 0
        );
        """
        
        var createTableStatement: OpaquePointer? = nil
        if sqlite3_prepare_v2(db, createTableString, -1, &createTableStatement, nil) == SQLITE_OK {
            if sqlite3_step(createTableStatement) == SQLITE_DONE {
                // print("Locations table created.")
            } else {
                print("Locations table could not be created.")
            }
        } else {
            print("CREATE TABLE statement could not be prepared.")
        }
        sqlite3_finalize(createTableStatement)
    }
    
    func insertLocation(latitude: Double, longitude: Double, accuracy: Double, altitude: Double, speed: Double, bearing: Double, timestamp: Double, activity: String?, batteryLevel: Int?) {
        let insertStatementString = "INSERT INTO locations (latitude, longitude, accuracy, altitude, speed, bearing, timestamp, activity, battery_level, is_synced) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0);"
        var insertStatement: OpaquePointer? = nil
        
        if sqlite3_prepare_v2(db, insertStatementString, -1, &insertStatement, nil) == SQLITE_OK {
            sqlite3_bind_double(insertStatement, 1, latitude)
            sqlite3_bind_double(insertStatement, 2, longitude)
            sqlite3_bind_double(insertStatement, 3, accuracy)
            sqlite3_bind_double(insertStatement, 4, altitude)
            sqlite3_bind_double(insertStatement, 5, speed)
            sqlite3_bind_double(insertStatement, 6, bearing)
            sqlite3_bind_double(insertStatement, 7, timestamp)
            
            if let activity = activity {
                sqlite3_bind_text(insertStatement, 8, (activity as NSString).utf8String, -1, nil)
            } else {
                sqlite3_bind_null(insertStatement, 8)
            }
            
            if let batteryLevel = batteryLevel {
                sqlite3_bind_int(insertStatement, 9, Int32(batteryLevel))
            } else {
                sqlite3_bind_null(insertStatement, 9)
            }
            
            if sqlite3_step(insertStatement) == SQLITE_DONE {
                // print("Successfully inserted row.")
            } else {
                print("Could not insert row.")
            }
        } else {
            print("INSERT statement could not be prepared.")
        }
        sqlite3_finalize(insertStatement)
    }
    
    func getUnsyncedLocations(limit: Int) -> [[String: Any]] {
        let queryStatementString = "SELECT * FROM locations WHERE is_synced = 0 ORDER BY timestamp ASC LIMIT ?;"
        var queryStatement: OpaquePointer? = nil
        var locations : [[String: Any]] = []
        
        if sqlite3_prepare_v2(db, queryStatementString, -1, &queryStatement, nil) == SQLITE_OK {
            sqlite3_bind_int(queryStatement, 1, Int32(limit))
            
            while sqlite3_step(queryStatement) == SQLITE_ROW {
                let id = sqlite3_column_int64(queryStatement, 0)
                let latitude = sqlite3_column_double(queryStatement, 1)
                let longitude = sqlite3_column_double(queryStatement, 2)
                let accuracy = sqlite3_column_double(queryStatement, 3)
                let altitude = sqlite3_column_double(queryStatement, 4)
                let speed = sqlite3_column_double(queryStatement, 5)
                let bearing = sqlite3_column_double(queryStatement, 6)
                let timestamp = sqlite3_column_double(queryStatement, 7)
                
                var activity: String? = nil
                if let cActivity = sqlite3_column_text(queryStatement, 8) {
                    activity = String(cString: cActivity)
                }
                
                let batteryLevel = sqlite3_column_int(queryStatement, 9)
                
                let location: [String: Any] = [
                    "id": id,
                    "latitude": latitude,
                    "longitude": longitude,
                    "accuracy": accuracy,
                    "altitude": altitude,
                    "speed": speed,
                    "bearing": bearing,
                    "timestamp": Int(timestamp), // Dart expects millis usually, check this
                    "activity": activity ?? "UNKNOWN",
                    "batteryLevel": Int(batteryLevel)
                ]
                locations.append(location)
            }
        } else {
            print("SELECT statement could not be prepared")
        }
        sqlite3_finalize(queryStatement)
        return locations
    }
    
    func getLocationCount() -> Int {
        let queryStatementString = "SELECT COUNT(*) FROM locations;"
        var queryStatement: OpaquePointer? = nil
        var count = 0
        
        if sqlite3_prepare_v2(db, queryStatementString, -1, &queryStatement, nil) == SQLITE_OK {
            if sqlite3_step(queryStatement) == SQLITE_ROW {
                count = Int(sqlite3_column_int(queryStatement, 0))
            }
        }
        sqlite3_finalize(queryStatement)
        return count
    }
    
    func clearAll() {
        let deleteStatementString = "DELETE FROM locations;"
        var deleteStatement: OpaquePointer? = nil
        if sqlite3_prepare_v2(db, deleteStatementString, -1, &deleteStatement, nil) == SQLITE_OK {
            sqlite3_step(deleteStatement)
        }
        sqlite3_finalize(deleteStatement)
    }
    
    func markAsSynced(ids: [Int64]) {
        if ids.isEmpty { return }
        let idsString = ids.map { String($0) }.joined(separator: ",")
        let updateStatementString = "UPDATE locations SET is_synced = 1 WHERE id IN (\(idsString));"
        var updateStatement: OpaquePointer? = nil
        
        if sqlite3_prepare_v2(db, updateStatementString, -1, &updateStatement, nil) == SQLITE_OK {
            sqlite3_step(updateStatement)
        }
        sqlite3_finalize(updateStatement)
    }
    
    deinit {
        sqlite3_close(db)
    }
}
