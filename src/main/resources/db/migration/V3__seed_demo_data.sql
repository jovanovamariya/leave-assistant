-- Demo data for local development. Check the holiday dates against the official calendar before relying on them.

INSERT INTO leave_balance (leave_type, allowance_days, used_days) VALUES
    ('ANNUAL', 26, 0),
    ('SICK',   10, 0);

INSERT INTO public_holiday (holiday_date, name) VALUES
    ('2026-11-01', 'All Saints'' Day'),
    ('2026-11-11', 'Independence Day'),
    ('2026-12-24', 'Christmas Eve'),
    ('2026-12-25', 'Christmas Day'),
    ('2026-12-26', 'Second Day of Christmas'),
    ('2027-01-01', 'New Year''s Day'),
    ('2027-01-06', 'Epiphany');
