INSERT INTO tasks (name, cost, description) VALUES ('Чистка надгробия', '1200','Тщательная очистка надгробия от мха и прочего');
insert into guests (fio, contacts, dateOfRegistration, login, password, balance) values ('Севостьянов Сергей Вячеславович', '79289018987', '2024-11-03', '1111', '1111', 15000);
INSERT INTO guests (id, fio, contacts, DATEOFREGISTRATION, login, password, balance) VALUES (2, 'Петров Петр Петрович', '123-456-7890', '2023-12-01', 'petrov', 'password123', 15000);

INSERT INTO orders (guest_id, order_name, order_description, order_cost, order_date)
VALUES (1, 'Уборка территории', 'Очистка территории от мусора', 500, '2024-11-25');
INSERT INTO orders (guest_id, order_name, order_description, order_cost, order_date)
VALUES (1, 'Уход за надгробием', 'Полировка до блеска', 1200, '2024-11-27');
INSERT INTO burials (guest_id, fio, death_date, birth_date) VALUES(1, 'Иванов Иван Иванович', '2023-01-15', '1980-05-20'),
                                                      (1, 'Петрова Анна Сергеевна', '2023-02-10', '1990-07-30');
INSERT INTO burials (guest_id, fio, death_date, birth_date)
VALUES (2, 'Иванов Иван Иванович', '1980-05-15', '1950-10-10');