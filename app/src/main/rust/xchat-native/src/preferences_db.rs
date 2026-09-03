use libsql::{Builder, Connection, Value};
use std::sync::OnceLock;
use tokio::sync::Mutex;

pub const TYPE_BOOL: i32 = 1;
pub const TYPE_INT: i32 = 2;
pub const TYPE_LONG: i32 = 3;
pub const TYPE_FLOAT: i32 = 4;
pub const TYPE_STRING: i32 = 5;
pub const TYPE_STRING_SET: i32 = 6;
pub const TYPE_BYTES: i32 = 7;
pub const TYPE_SERIALIZABLE: i32 = 8;

pub struct DBRow {
    pub val_type: i32,
    pub val_bool: Option<bool>,
    pub val_int: Option<i32>,
    pub val_long: Option<i64>,
    pub val_float: Option<f32>,
    pub val_string: Option<String>,
    pub val_bytes: Option<Vec<u8>>,
}

static TOKIO_RT: OnceLock<tokio::runtime::Runtime> = OnceLock::new();
static DB_CONN: OnceLock<Mutex<Connection>> = OnceLock::new();

fn get_rt() -> &'static tokio::runtime::Runtime {
    TOKIO_RT.get_or_init(|| {
        tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("Failed to build tokio runtime")
    })
}

fn block_on<F: std::future::Future>(future: F) -> F::Output {
    get_rt().block_on(future)
}

pub fn init_db(db_path: &str) {
    DB_CONN.get_or_init(|| {
        block_on(async {
            let db = Builder::new_local(db_path)
                .build()
                .await
                .expect("Failed to open libsql database");
            let conn = db.connect().expect("Failed to connect to database");
            conn.execute(
                "CREATE TABLE IF NOT EXISTS preferences (
                    key TEXT PRIMARY KEY,
                    type INTEGER NOT NULL,
                    val_bool INTEGER,
                    val_int INTEGER,
                    val_long INTEGER,
                    val_float REAL,
                    val_string TEXT,
                    val_bytes BLOB
                )",
                (),
            )
            .await
            .expect("Failed to create preferences table");
            Mutex::new(conn)
        })
    });
}

#[allow(clippy::too_many_arguments)]
pub fn put_value(
    key: &str,
    val_type: i32,
    val_bool: Option<bool>,
    val_int: Option<i32>,
    val_long: Option<i64>,
    val_float: Option<f32>,
    val_string: Option<&str>,
    val_bytes: Option<&[u8]>,
) {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        let sql = "INSERT INTO preferences (key, type, val_bool, val_int, val_long, val_float, val_string, val_bytes)
                   VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                   ON CONFLICT(key) DO UPDATE SET
                       type = excluded.type,
                       val_bool = excluded.val_bool,
                       val_int = excluded.val_int,
                       val_long = excluded.val_long,
                       val_float = excluded.val_float,
                       val_string = excluded.val_string,
                       val_bytes = excluded.val_bytes";

        let val_bool_val = val_bool.map(|b| if b { 1i64 } else { 0i64 });
        let val_int_val = val_int.map(|i| i as i64);
        let val_long_val = val_long;
        let val_float_val = val_float.map(|f| f as f64);

        let v_key = Value::Text(key.to_string());
        let v_type = Value::Integer(val_type as i64);
        let v_bool = match val_bool_val {
            Some(b) => Value::Integer(b),
            None => Value::Null,
        };
        let v_int = match val_int_val {
            Some(i) => Value::Integer(i),
            None => Value::Null,
        };
        let v_long = match val_long_val {
            Some(l) => Value::Integer(l),
            None => Value::Null,
        };
        let v_float = match val_float_val {
            Some(f) => Value::Real(f),
            None => Value::Null,
        };
        let v_string = match val_string {
            Some(s) => Value::Text(s.to_string()),
            None => Value::Null,
        };
        let v_bytes = match val_bytes {
            Some(b) => Value::Blob(b.to_vec()),
            None => Value::Null,
        };

        conn.execute(
            sql,
            (
                v_key, v_type, v_bool, v_int, v_long, v_float, v_string, v_bytes,
            ),
        )
        .await
        .expect("Failed to execute put query");
    });
}

pub fn get_value(key: &str) -> Option<DBRow> {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        let mut rows = conn
            .query(
                "SELECT type, val_bool, val_int, val_long, val_float, val_string, val_bytes FROM preferences WHERE key = ?1",
                [Value::Text(key.to_string())],
            )
            .await
            .expect("Failed to execute get query");

        if let Some(row) = rows.next().await.expect("Failed to read query result") {
            let val_type: i32 = row.get(0).expect("Failed to get type");
            let val_bool_raw: Option<i64> = row.get(1).expect("Failed to get val_bool");
            let val_int_raw: Option<i64> = row.get(2).expect("Failed to get val_int");
            let val_long_raw: Option<i64> = row.get(3).expect("Failed to get val_long");
            let val_float_raw: Option<f64> = row.get(4).expect("Failed to get val_float");
            let val_string: Option<String> = row.get(5).expect("Failed to get val_string");
            let val_bytes: Option<Vec<u8>> = row.get(6).expect("Failed to get val_bytes");

            Some(DBRow {
                val_type,
                val_bool: val_bool_raw.map(|b| b != 0),
                val_int: val_int_raw.map(|i| i as i32),
                val_long: val_long_raw,
                val_float: val_float_raw.map(|f| f as f32),
                val_string,
                val_bytes,
            })
        } else {
            None
        }
    })
}

pub fn contains_key(key: &str) -> bool {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        let mut rows = conn
            .query(
                "SELECT 1 FROM preferences WHERE key = ?1",
                [Value::Text(key.to_string())],
            )
            .await
            .expect("Failed to execute contains query");

        rows.next()
            .await
            .expect("Failed to read contains result")
            .is_some()
    })
}

pub fn remove_key(key: &str) {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        conn.execute(
            "DELETE FROM preferences WHERE key = ?1",
            [Value::Text(key.to_string())],
        )
        .await
        .expect("Failed to execute remove query");
    })
}

pub fn clear_db() {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        conn.execute("DELETE FROM preferences", ())
            .await
            .expect("Failed to execute clear query");
    })
}

pub fn get_all_keys() -> Vec<String> {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        let mut rows = conn
            .query("SELECT key FROM preferences", ())
            .await
            .expect("Failed to execute get_all_keys query");

        let mut keys = Vec::new();
        while let Some(row) = rows.next().await.expect("Failed to get next row") {
            let key: String = row.get(0).expect("Failed to get key");
            keys.push(key);
        }
        keys
    })
}

pub fn get_type(key: &str) -> i32 {
    block_on(async {
        let conn_mutex = DB_CONN.get().expect("Database not initialized");
        let conn = conn_mutex.lock().await;

        let mut rows = conn
            .query(
                "SELECT type FROM preferences WHERE key = ?1",
                [Value::Text(key.to_string())],
            )
            .await
            .expect("Failed to execute get_type query");

        if let Some(row) = rows.next().await.expect("Failed to get next row") {
            row.get(0).expect("Failed to get type")
        } else {
            0
        }
    })
}
