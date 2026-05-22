const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Local standard database storage for instant plug-and-play testing
const DB_PATH = path.join(__dirname, '..', 'dulcevision.db');

const db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) {
    console.error('Error connecting to the database:', err.message);
  } else {
    console.log('Connected to the SQLite/PostgreSQL-compatible Database.');
  }
});

// Initialize Commercial Schema Tables
function initSchema() {
  db.serialize(() => {
    // 1. Users table
    db.run(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        createdAt TEXT DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // 2. User Profiles table
    db.run(`
      CREATE TABLE IF NOT EXISTS profiles (
        id TEXT PRIMARY KEY,
        userId TEXT NOT NULL,
        name TEXT NOT NULL,
        avatarUrl TEXT NOT NULL,
        isAdult INTEGER DEFAULT 1,
        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
      )
    `);

    // 3. Movies / Media VOD table
    db.run(`
      CREATE TABLE IF NOT EXISTS movies (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        thumbnail TEXT,
        backdrop TEXT,
        videoUrl TEXT NOT NULL,
        duration TEXT,
        genre TEXT,
        year INTEGER,
        rating REAL,
        description TEXT,
        isTrending INTEGER DEFAULT 0,
        isPopular INTEGER DEFAULT 0
      )
    `);

    // 4. Series table
    db.run(`
      CREATE TABLE IF NOT EXISTS series (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        thumbnail TEXT,
        backdrop TEXT,
        genres TEXT,
        rating REAL,
        description TEXT,
        year INTEGER
      )
    `);

    // 5. Seasons table
    db.run(`
      CREATE TABLE IF NOT EXISTS seasons (
        id TEXT PRIMARY KEY,
        seriesId TEXT NOT NULL,
        number INTEGER,
        title TEXT,
        FOREIGN KEY(seriesId) REFERENCES series(id) ON DELETE CASCADE
      )
    `);

    // 6. Episodes table
    db.run(`
      CREATE TABLE IF NOT EXISTS episodes (
        id TEXT PRIMARY KEY,
        seasonId TEXT NOT NULL,
        seriesId TEXT NOT NULL,
        number INTEGER,
        title TEXT NOT NULL,
        thumbnail TEXT,
        videoUrl TEXT NOT NULL,
        duration TEXT,
        description TEXT,
        FOREIGN KEY(seasonId) REFERENCES seasons(id) ON DELETE CASCADE,
        FOREIGN KEY(seriesId) REFERENCES series(id) ON DELETE CASCADE
      )
    `);

    // 7. IPTV Channels table
    db.run(`
      CREATE TABLE IF NOT EXISTS channels (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        streamUrl TEXT NOT NULL,
        logoUrl TEXT,
        category TEXT,
        epgTitle TEXT,
        epgTimeCode TEXT,
        epgNextTitle TEXT,
        isFavorite INTEGER DEFAULT 0
      )
    `);

    // 8. Watch history tracking table
    db.run(`
      CREATE TABLE IF NOT EXISTS watch_progress (
        userId TEXT NOT NULL,
        mediaId TEXT PRIMARY KEY,
        mediaType TEXT NOT NULL,
        title TEXT,
        detailText TEXT,
        thumbnail TEXT,
        positionMs INTEGER,
        durationMs INTEGER,
        timestamp INTEGER
      )
    `);

    // Seed dummy commercial sample datasets if empty
    db.get("SELECT COUNT(*) as count FROM movies", (err, row) => {
      if (row && row.count === 0) {
        console.log('Seeding initial media database catalog...');
        
        // Seed Sintel Movie
        db.run(`
          INSERT INTO movies (id, title, thumbnail, backdrop, videoUrl, duration, genre, year, rating, description, isTrending, isPopular)
          VALUES (
            'mov_sintel',
            'Sintel (The Journey)',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg',
            'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
            '14:48',
            'Fantasía / Aventuras',
            2024,
            8.7,
            'Una niña busca desesperadamente a su fiel compañero de viaje, un pequeño dragón llamado Scales, a través de imponentes tundras nevadas y peligrosos páramos desérticos en una odisea cinematográfica de animación sin precedentes.',
            1, 0
          )
        `);

        // Seed Tears Of Steel Movie
        db.run(`
          INSERT INTO movies (id, title, thumbnail, backdrop, videoUrl, duration, genre, year, rating, description, isTrending, isPopular)
          VALUES (
            'mov_tears_of_steel',
            'Tears of Steel (Sci-Fi)',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg',
            'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
            '12:14',
            'Sci-Fi / Cyberpunk',
            2025,
            9.2,
            'Ubicada en un futuro alternativo en Ámsterdam, un grupo de científicos intenta salvar la civilización utilizando tecnología de rejuvenecimiento holográfico y un robot gigantesco manipulado por el amor perdido de una joven ciborg.',
            1, 1
          )
        `);

        // Seed Big Buck Bunny Movie
        db.run(`
          INSERT INTO movies (id, title, thumbnail, backdrop, videoUrl, duration, genre, year, rating, description, isTrending, isPopular)
          VALUES (
            'mov_big_buck_bunny',
            'Big Buck Bunny (La Venganza)',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg',
            'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
            '09:56',
            'Animación / Humor',
            2023,
            7.9,
            'Un conejo gigante de carácter apacible toma la justicia por su mano cuando tres malévolos roedores del bosque perturban la paz arrojándole bellotas y saboteando la flora de su hermoso jardín.',
            0, 1
          )
        `);
      }
    });

    // Seed Series
    db.get("SELECT COUNT(*) as count FROM series", (err, row) => {
      if (row && row.count === 0) {
        console.log('Seeding initial series, seasons, and episodes catalog...');

        const seriesId1 = "ser_sintel_revelations";
        db.run(`
          INSERT INTO series (id, title, thumbnail, backdrop, genres, rating, description, year)
          VALUES (
            '${seriesId1}',
            'Sintel: Revelaciones',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg',
            'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800',
            'Fantasía / Acción',
            9.4,
            'La serie oficial derivada del aclamado cortometraje Sintel, que profundiza en la historia del templo sagrado, el adiestramiento de dragones y el retorno de la magia ancestral.',
            2025
          )
        `);

        const seriesId2 = "ser_cyberpunk_tears";
        db.run(`
          INSERT INTO series (id, title, thumbnail, backdrop, genres, rating, description, year)
          VALUES (
            '${seriesId2}',
            'Tears of Steel: Cyber Edge',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg',
            'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800',
            'Sci-Fi / Thriller',
            8.9,
            'Ubicada diez años antes de los eventos fundamentales, la serie examina la génesis de Celia y el desarrollo científico de los robots de defensa que rigen la megalópolis futurista.',
            2026
          )
        `);

        // Seed Seasons & Episodes for Sintel
        const seasonId1 = "seasm_ser_sintel_revelations_1";
        db.run(`INSERT INTO seasons (id, seriesId, number, title) VALUES ('${seasonId1}', '${seriesId1}', 1, 'Temporada 1')`);
        
        db.run(`
          INSERT INTO episodes (id, seasonId, seriesId, number, title, thumbnail, videoUrl, duration, description)
          VALUES (
            '${seasonId1}_ep1',
            '${seasonId1}',
            '${seriesId1}',
            1,
            'La Caída del Reino Sagrado',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
            '14:48 min',
            'Un viaje audaz que sacudirá los cimientos del antiguo dragón Scales.'
          )
        `);
        db.run(`
          INSERT INTO episodes (id, seasonId, seriesId, number, title, thumbnail, videoUrl, duration, description)
          VALUES (
            '${seasonId1}_ep2',
            '${seasonId1}',
            '${seriesId1}',
            2,
            'Senda Cibernética',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
            '12:14 min',
            'Un laberinto tecnológico repleto de peligros y revelaciones sobre la vida de Celia.'
          )
        `);

        const seasonId2 = "seasm_ser_cyberpunk_tears_1";
        db.run(`INSERT INTO seasons (id, seriesId, number, title) VALUES ('${seasonId2}', '${seriesId2}', 1, 'Temporada 1')`);
        
        db.run(`
          INSERT INTO episodes (id, seasonId, seriesId, number, title, thumbnail, videoUrl, duration, description)
          VALUES (
            '${seasonId2}_ep1',
            '${seasonId2}',
            '${seriesId2}',
            1,
            'Origen y Revelación',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg',
            'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
            '12:14 min',
            'El primer episodio desvela las raíces del conflicto y los desafíos a los que nuestros héroes deberán enfrentarse.'
          )
        `);
      }
    });

    // Seed channels if empty
    db.get("SELECT COUNT(*) as count FROM channels", (err, row) => {
      if (row && row.count === 0) {
        db.run(`
          INSERT INTO channels (id, name, streamUrl, logoUrl, category, epgTitle, epgTimeCode, epgNextTitle, isFavorite)
          VALUES (
            'ch_cinema_prem',
            'Cinema Premium HD',
            'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8',
            'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=200',
            'Cine & Películas',
            'Tears of Steel en Vivo',
            '20:00 - 20:15',
            'Big Buck Bunny (Dibujos)',
            1
          )
        `);
      }
    });
  });
}

module.exports = {
  db,
  initSchema
};
