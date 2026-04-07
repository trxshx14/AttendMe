
export const getWeatherInfo =
    (code, isDay = 1) => {
  if (code === 0)
    return { label: 'Clear Sky',
      emoji: isDay ? '☀️' : '🌙' };
  if ([1,2].includes(code))
    return { label:'Partly Cloudy',
          emoji:'⛅' };
  if ([61,63,65].includes(code))
    return { label:'Rain', emoji:'🌧️' };
  if ([95,96,99].includes(code))
    return { label:'Thunderstorm',
          emoji:'⛈️' };
  return { label:'Unknown', emoji:'🌡️' };
};

export const getAttendanceTip =
    (code, temp) => {
  if ([95,96,99].includes(code))
    return { icon:'⛈️', level:'High',
      color:'#DC2626', bg:'#FEF2F2',
      tip:'Thunderstorm — expect absences.' };
  if ([61,63,65,80,81,82].includes(code))
    return { icon:'🌧️', level:'Moderate',
      color:'#D97706', bg:'#FFFBEB',
      tip:'Rainy — transport issues.' };
  return { icon:'✅', level:'No Impact',
    color:'#059669', bg:'#F0FDF4',
    tip:'Good weather today!' };
};