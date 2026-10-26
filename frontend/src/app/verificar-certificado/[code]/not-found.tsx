import Link from "next/link";
export default function NotFound() {
  return <main className="mx-auto max-w-xl space-y-4 p-8"><h1 className="text-2xl font-semibold">Certificado não encontrado</h1>
    <p>Confira o código informado no documento.</p><Link className="underline" href="/verificar-certificado">Consultar outro código</Link></main>;
}
