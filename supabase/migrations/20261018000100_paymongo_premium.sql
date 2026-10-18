-- TransitPH Premium: PayMongo checkout records and server-only activation.
alter table if exists public.profiles
  add column if not exists premium_until timestamptz;

create table if not exists public.premium_payment_orders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  reference_number text not null unique,
  checkout_session_id text unique,
  payment_id text,
  plan_type text not null default 'PREMIUM_MONTHLY',
  amount_minor integer not null check (amount_minor > 0),
  currency text not null default 'PHP' check (currency = 'PHP'),
  status text not null default 'pending' check (status in ('pending', 'paid', 'failed')),
  created_at timestamptz not null default now(),
  paid_at timestamptz
);

alter table public.premium_payment_orders enable row level security;

drop policy if exists "Users can read their own premium payment orders" on public.premium_payment_orders;
create policy "Users can read their own premium payment orders"
  on public.premium_payment_orders for select
  to authenticated
  using (auth.uid() = user_id);

revoke insert, update, delete on public.premium_payment_orders from anon, authenticated;

create or replace function public.activate_premium_from_paymongo(
  p_reference_number text,
  p_checkout_session_id text,
  p_payment_id text,
  p_paid_amount integer,
  p_currency text
) returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  payment_order public.premium_payment_orders%rowtype;
begin
  select * into payment_order
  from public.premium_payment_orders
  where reference_number = p_reference_number
  for update;

  if not found then
    return false;
  end if;

  if payment_order.status = 'paid' then
    return true;
  end if;

  if payment_order.checkout_session_id is distinct from p_checkout_session_id
     or payment_order.amount_minor <> p_paid_amount
     or payment_order.currency <> upper(p_currency) then
    return false;
  end if;

  update public.profiles
  set premium_until = greatest(coalesce(premium_until, now()), now()) + interval '30 days'
  where id = payment_order.user_id;

  if not found then
    raise exception 'TransitPH profile missing for paid user %', payment_order.user_id;
  end if;

  update public.premium_payment_orders
  set status = 'paid',
      payment_id = p_payment_id,
      paid_at = now()
  where id = payment_order.id;

  return true;
end;
$$;

revoke all on function public.activate_premium_from_paymongo(text, text, text, integer, text) from public, anon, authenticated;
grant execute on function public.activate_premium_from_paymongo(text, text, text, integer, text) to service_role;