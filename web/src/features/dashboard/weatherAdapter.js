export const getWeatherInfo = (code, isDay = 1) => {
  if (code === 0)
    return { label: 'Clear Sky', emoji: isDay ? '☀️' : '🌙' };
  if ([1, 2].includes(code))
    return { label: 'Partly Cloudy', emoji: '⛅' };
  if (code === 3)
    return { label: 'Overcast', emoji: '☁️' };
  if ([45, 48].includes(code))
    return { label: 'Foggy', emoji: '🌫️' };
  if ([51, 53, 55].includes(code))
    return { label: 'Drizzle', emoji: '🌦️' };
  if ([61, 63, 65].includes(code))
    return { label: 'Rain', emoji: '🌧️' };
  if ([80, 81, 82].includes(code))
    return { label: 'Rain Showers', emoji: '🌧️' };
  if ([95, 96, 99].includes(code))
    return { label: 'Thunderstorm', emoji: '⛈️' };
  return { label: 'Unknown', emoji: '🌡️' };
};

export const getAttendanceTip = (code, temp) => {
  if ([95, 96, 99].includes(code))
    return {
      icon: '⛈️',
      level: 'High Impact',
      color: '#DC2626',
      bg: '#FEF2F2',
      tip: 'Thunderstorm — expect significant absences. Consider remote options.',
    };
  if ([61, 63, 65, 80, 81, 82].includes(code))
    return {
      icon: '🌧️',
      level: 'Moderate Impact',
      color: '#D97706',
      bg: '#FFFBEB',
      tip: 'Rainy conditions — transport issues likely. Some absences expected.',
    };
  if ([45, 48, 51, 53, 55].includes(code))
    return {
      icon: '🌫️',
      level: 'Low Impact',
      color: '#6366F1',
      bg: '#EEF2FF',
      tip: 'Light drizzle or fog — minor delays possible.',
    };
  if (temp >= 38)
    return {
      icon: '🌡️',
      level: 'Heat Advisory',
      color: '#EA580C',
      bg: '#FFF7ED',
      tip: 'Extreme heat — students may be affected. Ensure hydration.',
    };
  return {
    icon: '✅',
    level: 'No Impact',
    color: '#059669',
    bg: '#F0FDF4',
    tip: 'Good weather today! High attendance expected.',
  };
};