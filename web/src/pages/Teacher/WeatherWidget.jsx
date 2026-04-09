import React, { useState, useEffect } from 'react';
import { MapPin, Wind, Droplets, RefreshCw, AlertTriangle } from 'lucide-react';
import './WeatherWidget.css';
import { getWeatherInfo, getAttendanceTip } from '../../services/weatherAdapter';


const WeatherWidget = () => {
  const [weather, setWeather]         = useState(null);
  const [location, setLocation]       = useState('');
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState('');
  const [lastUpdated, setLastUpdated] = useState('');

  useEffect(() => { fetchWeather(); }, []);

  const fetchWeather = async () => {
    setLoading(true);
    setError('');
    try {
      /* Step 1 — Get coordinates via Geolocation API
         Falls back to Cebu City (CIT-U location) if user denies */
      const coords = await new Promise((resolve) => {
        if (!navigator.geolocation) {
          resolve({ lat: 10.3157, lon: 123.8854 });
          return;
        }
        navigator.geolocation.getCurrentPosition(
          pos  => resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
          ()   => resolve({ lat: 10.3157, lon: 123.8854 })
        );
      });

      /* Step 2 — Reverse geocode with OpenStreetMap Nominatim
         Free, no API key, returns city/municipality name */
      const geoRes  = await fetch(
        `https://nominatim.openstreetmap.org/reverse?lat=${coords.lat}&lon=${coords.lon}&format=json`,
        { headers: { 'Accept-Language': 'en' } }
      );
      const geoData = await geoRes.json();
      const city    = geoData.address?.city
        || geoData.address?.town
        || geoData.address?.municipality
        || geoData.address?.county
        || 'Your Location';
      setLocation(city);

      /* Step 3 — Fetch weather from Open-Meteo
         100% free REST API, no API key required
         Docs: https://open-meteo.com/en/docs */
      const weatherRes  = await fetch(
        `https://api.open-meteo.com/v1/forecast` +
        `?latitude=${coords.lat}&longitude=${coords.lon}` +
        `&current=temperature_2m,relative_humidity_2m,apparent_temperature,` +
        `weather_code,wind_speed_10m,visibility,is_day` +
        `&wind_speed_unit=kmh&temperature_unit=celsius&timezone=auto`
      );
      const weatherData = await weatherRes.json();
      const cur         = weatherData.current;

      setWeather({
        temp:       Math.round(cur.temperature_2m),
        feelsLike:  Math.round(cur.apparent_temperature),
        humidity:   cur.relative_humidity_2m,
        wind:       Math.round(cur.wind_speed_10m),
        visibility: cur.visibility != null ? Math.round(cur.visibility / 1000) : null,
        code:       cur.weather_code,
        isDay:      cur.is_day,
      });
      setLastUpdated(
        new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
      );
    } catch {
      setError('Unable to load weather data. Check your internet connection.');
    } finally {
      setLoading(false);
    }
  };

  /* ── Loading ──────────────────────────────────────────── */
  if (loading) {
    return (
      <div className="wd-card wd-loading-state">
        <div className="wd-spinner" />
        <span className="wd-loading-text">Fetching weather data...</span>
      </div>
    );
  }

  /* ── Error ────────────────────────────────────────────── */
  if (error) {
    return (
      <div className="wd-card wd-error-state">
        <AlertTriangle size={20} color="#D97706" />
        <span>{error}</span>
        <button className="wd-refresh-btn" onClick={fetchWeather}>
          <RefreshCw size={13} /> Retry
        </button>
      </div>
    );
  }

  const { label, emoji }                         = getWeatherInfo(weather.code, weather.isDay);
  const { tip, color, bg, icon: tipIcon, level } = getAttendanceTip(weather.code, weather.temp);

  return (
    <div className="wd-card">

      {/* ── Weather Info ─────────────────────────────── */}
      <div className="wd-main">
        <span className="wd-emoji">{emoji}</span>

        <div className="wd-temp-block">
          <div className="wd-temp">
            {weather.temp}°<span className="wd-unit">C</span>
          </div>
          <div className="wd-condition">{label}</div>
          <div className="wd-location">
            <MapPin size={12} strokeWidth={2.5} />
            {location}
          </div>
        </div>

        <div className="wd-details-grid">
          <div className="wd-detail">
            <Droplets size={15} color="#3B82F6" />
            <div>
              <span className="wd-detail-val">{weather.humidity}%</span>
              <span className="wd-detail-lbl">Humidity</span>
            </div>
          </div>
          <div className="wd-detail">
            <Wind size={15} color="#64748B" />
            <div>
              <span className="wd-detail-val">{weather.wind} km/h</span>
              <span className="wd-detail-lbl">Wind</span>
            </div>
          </div>
          <div className="wd-detail">
            <span className="wd-detail-emoji">🌡️</span>
            <div>
              <span className="wd-detail-val">{weather.feelsLike}°C</span>
              <span className="wd-detail-lbl">Feels like</span>
            </div>
          </div>
          {weather.visibility !== null && (
            <div className="wd-detail">
              <span className="wd-detail-emoji">👁️</span>
              <div>
                <span className="wd-detail-val">{weather.visibility} km</span>
                <span className="wd-detail-lbl">Visibility</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Divider ──────────────────────────────────── */}
      <div className="wd-divider" />

      {/* ── Attendance Tip ───────────────────────────── */}
      <div className="wd-tip" style={{ background: bg }}>
        <div className="wd-tip-header">
          <span className="wd-tip-emoji">{tipIcon}</span>
          <div>
            <div className="wd-tip-title" style={{ color }}>Attendance Outlook</div>
            <div className="wd-tip-level" style={{ color }}>{level}</div>
          </div>
        </div>
        <p className="wd-tip-text" style={{ color }}>{tip}</p>
        <div className="wd-tip-footer">
          <span className="wd-updated">Updated {lastUpdated}</span>
          <button className="wd-refresh-btn" onClick={fetchWeather}>
            <RefreshCw size={12} /> Refresh
          </button>
        </div>
      </div>

    </div>
  );
};

export default WeatherWidget;