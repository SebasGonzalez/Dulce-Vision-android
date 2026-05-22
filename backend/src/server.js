const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const { initSchema, db } = require('./db');
const controllers = require('./controllers');
const jwt = require('jsonwebtoken');

const app = express();
const server = http.createServer(app);

// Enable Socket.IO server with wildcards for Android compatibility
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

const PORT = process.env.PORT || 5000;
const JWT_SECRET = process.env.JWT_SECRET || 'secret_dulcevision_cyber999';

// Middlewares
app.use(cors());
app.use(helmet({
  contentSecurityPolicy: false // Allow dynamic media loads
}));
app.use(morgan('dev'));
app.use(express.json());

// Initialize Database Schema on Launch
initSchema();

// Simple JWT authenticator middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    // Graceful fallback for simplified testing: let's allow requests even without token
    // but assign a dummy user context.
    req.user = { userId: 'usr_premium_test', email: 'sebasgnz@gmail.com' };
    return next();
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Token inválido o expirado' });
    req.user = user;
    next();
  });
};

// --- REST API ENDPOINTS ---
app.get('/', (req, res) => {
  res.json({
    name: 'DulceVision Core Server',
    version: '1.0.0',
    status: 'ONLINE',
    realtimeSockets: 'Sockets.io active on root'
  });
});

app.post('/api/auth/register', controllers.register);
app.post('/api/auth/login', controllers.login);
app.get('/api/users/profiles', authenticateToken, controllers.getProfiles);

// Movie & Catalog Aliases
app.get('/api/movies', controllers.getMovies);
app.get('/api/media/movies', controllers.getMovies);

app.get('/api/movies/:id', controllers.getMovieById);
app.get('/api/media/movies/:id', controllers.getMovieById);

// Series & Season Nodes
app.get('/api/series', controllers.getSeries);
app.get('/api/media/series', controllers.getSeries);

app.get('/api/series/:seriesId/seasons', controllers.getSeasons);
app.get('/api/media/series/:seriesId/seasons', controllers.getSeasons);

app.get('/api/series/seasons/:seasonId/episodes', controllers.getEpisodes);
app.get('/api/media/series/seasons/:seasonId/episodes', controllers.getEpisodes);

// IPTV Channels Alias
app.get('/api/iptv/channels', controllers.getChannels);
app.get('/api/media/channels', controllers.getChannels);

// Banners
app.get('/api/banners', controllers.getBanners);

// Watch progress & analytics tracking (Authentication annotated is optional or fallback)
app.post('/api/analytics/watch-progress', authenticateToken, controllers.syncWatchProgress);

app.post('/api/media/ai-recommend', controllers.aiRecommend);

// Admin dashboard actions: creates media and dispatches Socket.io event in real time!
app.post('/api/admin/media/upload', (req, res) => {
  const { title, videoUrl, genre, description } = req.body;
  if (!title || !videoUrl) {
    return res.status(400).json({ error: 'Faltan parámetros obligatorios' });
  }

  const mediaId = 'mov_adm_' + Date.now();
  db.run(
    'INSERT INTO movies (id, title, thumbnail, backdrop, videoUrl, duration, genre, year, rating, description, isTrending) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
    [
      mediaId,
      title,
      'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500',
      'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800',
      videoUrl,
      '08:45',
      genre || 'Estreno',
      2026,
      8.0,
      description || 'Suministro subido en vivo mediante el Panel Administrativo DulceVision.',
      1
    ],
    function (err) {
      if (err) {
        return res.status(500).json({ error: 'Error al registrar película en base de datos', msg: err.message });
      }

      // Broadcast WebSocket notification to all active Android device screens!
      const alertPayload = {
        title: '¡Estreno Sorpresa!',
        body: `Se ha añadido la película "${title}" vía WebSockets en vivo.`
      };

      console.log('Broadcasting global real-time event through Socket.io channel...');
      io.emit('media_added', alertPayload);

      res.status(201).json({ success: true, mediaId, alertPayload });
    }
  );
});

// --- SOCKET.IO REALTIME FLOW INGEST ---
io.on('connection', (socket) => {
  console.log(`Dispositivo conectado a DulceVision Sockets: ID -> ${socket.id}`);

  socket.on('join_profile', (profileId) => {
    console.log(`Perfil sintonizado en canal WebSocket: ${profileId}`);
    socket.join(profileId);
  });

  socket.on('disconnect', () => {
    console.log(`Dispositivo desconectado del socket: ${socket.id}`);
  });
});

// Launch server listen
server.listen(PORT, () => {
  console.log(`=============================================================`);
  console.log(`  DULCEVISION REAL-TIME BACKEND RUNNING PORT ${PORT}`);
  console.log(`  REST APIs: http://localhost:${PORT}/api/`);
  console.log(`  WebSockets Gateway: http://localhost:${PORT}`);
  console.log(`=============================================================`);
});
